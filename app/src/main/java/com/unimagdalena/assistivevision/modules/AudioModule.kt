package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.unimagdalena.assistivevision.model.TtsVoiceCatalog
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fully offline text-to-speech built on the bundled sherpa-onnx engine.
 *
 * Both the runtime (`libsherpa-onnx-jni` + onnxruntime) and one voice model
 * per supported language ship inside the APK; playback goes through a local
 * [AudioTrack]. There is no platform TTS and no network access, so voice
 * output works identically in airplane mode.
 *
 * Responsibilities:
 *  - lazily initialising the bundled voice for the selected language,
 *  - streaming generated PCM to an [AudioTrack] (no full-buffer latency),
 *  - switching voices when the app language changes,
 *  - interrupting playback via [stop],
 *  - releasing the engine via [shutdown] when the owner is cleared.
 *
 * The constructor starts voice initialisation on a private executor; [speak]
 * tasks are serialised behind it, so the first call after a cold start simply
 * waits for the engine to come up.
 */
class AudioModule(context: Context) {

    /** Per-utterance abort flag so QUEUE_FLUSH-style interruption is race-free. */
    private class Utterance {
        val aborted = AtomicBoolean(false)
    }

    private val appContext: Context = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AudioModule-TTS").apply { isDaemon = false }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Optional observer notified on the main thread. */
    @Volatile
    var listener: Listener? = null

    @Volatile
    private var language: String = TtsVoiceCatalog.DEFAULT_LANGUAGE

    @Volatile
    private var speechRate: Float = 1.0f

    @Volatile
    private var ready: Boolean = false

    /** Language whose voice is currently loaded in the native engine. */
    @Volatile
    private var readyLanguage: String? = null

    @Volatile
    private var failed: Boolean = false

    @Volatile
    private var currentUtterance: Utterance? = null

    /** Executor-confined; do not touch from other threads. */
    private var tts: OfflineTts? = null

    /** Executor-confined; one track per utterance (sample rate is per voice). */
    private var track: AudioTrack? = null

    private var focusRequest: AudioFocusRequest? = null

    private val speechAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        executor.execute {
            try {
                ensureVoice(language)
                ready = true
                postReady()
            } catch (t: Throwable) {
                Log.e(TAG, "Offline TTS init failed", t)
                failed = true
                postError()
            }
        }
    }

    /** Sets the app language tag ("es" or "en"); the voice is swapped lazily. */
    fun setLanguage(language: String) {
        this.language = language
        if (failed) {
            // Previous init failed: retry with the newly requested voice.
            executor.execute {
                try {
                    ensureVoice(language)
                    ready = true
                    failed = false
                    postReady()
                } catch (t: Throwable) {
                    Log.e(TAG, "Offline TTS init retry failed", t)
                    failed = true
                    postError()
                }
            }
        }
    }

    /** Sets the speech rate multiplier (0.5f..2.0f from settings). */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    /**
     * Interrupts any current speech and announces [text].
     *
     * @return true when the utterance was queued and [Listener.onDone] or
     * [Listener.onError] will eventually fire; false only after shutdown.
     */
    fun speak(text: String): Boolean {
        val utterance = Utterance()
        currentUtterance?.aborted?.set(true)
        currentUtterance = utterance
        return try {
            executor.execute { runSpeak(text, utterance) }
            true
        } catch (_: RejectedExecutionException) {
            false
        }
    }

    /** Stops any ongoing speech and flushes playback. */
    fun stop() {
        currentUtterance?.aborted?.set(true)
        try {
            executor.execute { releaseTrack() }
        } catch (_: RejectedExecutionException) {
            // Shutting down; nothing to stop.
        }
    }

    /** Releases the engine. Must be called when the owner is cleared. */
    fun shutdown() {
        listener = null
        currentUtterance?.aborted?.set(true)
        try {
            executor.execute {
                releaseTrack()
                abandonAudioFocus()
                try {
                    tts?.release()
                } catch (t: Throwable) {
                    Log.w(TAG, "TTS release failed", t)
                }
                tts = null
                ready = false
                readyLanguage = null
            }
            executor.shutdown()
        } catch (_: RejectedExecutionException) {
            // Already shut down.
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    // ------------------------------------------------------------------
    // Executor-confined internals
    // ------------------------------------------------------------------

    private fun runSpeak(text: String, utterance: Utterance) {
        try {
            val engine = ensureVoice(language)
            ready = true
            readyLanguage = language
            postReady()

            val newTrack = createTrack(engine.sampleRate())
            if (newTrack == null) {
                Log.e(TAG, "AudioTrack init failed; voice output unavailable")
                failed = true
                postError()
                return
            }
            track = newTrack
            requestAudioFocus()
            newTrack.play()

            val started = SystemClock.elapsedRealtime()
            val audio = engine.generateWithCallback(
                text = text,
                speed = speechRate,
            ) { samples ->
                if (utterance.aborted.get()) return@generateWithCallback 0
                val t = track
                if (t == null || t.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    return@generateWithCallback 0
                }
                t.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                if (utterance.aborted.get()) 0 else 1
            }
            Log.i(
                TAG,
                "Generated ${audio.samples.size} samples in " +
                    "${SystemClock.elapsedRealtime() - started} ms"
            )

            if (utterance.aborted.get()) return
            drain(audio.samples.size)
            postDone()
        } catch (t: Throwable) {
            Log.e(TAG, "Speech generation failed", t)
            failed = true
            postError()
        } finally {
            releaseTrack()
            abandonAudioFocus()
        }
    }

    /** Blocks until all queued PCM has been played, or the utterance is aborted. */
    private fun drain(totalSamples: Int) {
        val t = track ?: return
        val deadline = SystemClock.elapsedRealtime() + MAX_DRAIN_WAIT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (currentUtterance?.aborted?.get() == true) return
            if (t.playbackHeadPosition >= totalSamples) return
            try {
                Thread.sleep(DRAIN_POLL_MS)
            } catch (_: InterruptedException) {
                return
            }
        }
        Log.w(TAG, "AudioTrack drain timed out")
    }

    private fun ensureVoice(languageTag: String): OfflineTts {
        val existing = tts
        if (existing != null && readyLanguage == languageTag) return existing

        existing?.let { current ->
            try {
                current.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Previous voice release failed", t)
            }
        }
        tts = null
        releaseTrack()

        val spec = TtsVoiceCatalog.specFor(languageTag)
            ?: throw IllegalArgumentException(
                "No bundled voice for language '$languageTag'"
            )
        val espeakDir = ensureEspeakDataOnDisk()
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "${spec.assetDir}/${spec.modelFile}",
                    tokens = "${spec.assetDir}/${spec.tokensFile}",
                    dataDir = espeakDir.absolutePath,
                ),
                numThreads = NUM_THREADS,
            ),
        )
        val started = SystemClock.elapsedRealtime()
        val engine = OfflineTts(assetManager = appContext.assets, config = config)
        tts = engine
        readyLanguage = languageTag
        Log.i(
            TAG,
            "Voice '${spec.assetDir}' ready in ${SystemClock.elapsedRealtime() - started} ms, " +
                "sampleRate=${engine.sampleRate()}"
        )
        return engine
    }

    /**
     * espeak-ng data cannot be read through the AssetManager by the native
     * code, so it is copied once from assets into internal storage. A marker
     * file re-triggers the copy when the bundled data version changes.
     */
    private fun ensureEspeakDataOnDisk(): File {
        val base = File(appContext.filesDir, "tts")
        val dest = File(base, "espeak-ng-data")
        val marker = File(base, "espeak-version.txt")
        val alreadyCopied = marker.isFile &&
            marker.readText().trim() == TtsVoiceCatalog.ESPEAK_DATA_VERSION.toString() &&
            dest.isDirectory
        if (!alreadyCopied) {
            Log.i(TAG, "Copying bundled espeak-ng data to ${dest.absolutePath}")
            dest.deleteRecursively()
            copyAssetDirectory(TtsVoiceCatalog.ESPEAK_DATA_ASSET_DIR, dest)
            marker.writeText(TtsVoiceCatalog.ESPEAK_DATA_VERSION.toString())
        }
        return dest
    }

    private fun copyAssetDirectory(assetPath: String, destDir: File) {
        destDir.mkdirs()
        val children = appContext.assets.list(assetPath).orEmpty()
        for (child in children) {
            val childAssetPath = "$assetPath/$child"
            val childDest = File(destDir, child)
            val nested = appContext.assets.list(childAssetPath).orEmpty()
            if (nested.isEmpty()) {
                appContext.assets.open(childAssetPath).use { input ->
                    FileOutputStream(childDest).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                copyAssetDirectory(childAssetPath, childDest)
            }
        }
    }

    private fun createTrack(sampleRate: Int): AudioTrack? {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        if (minBuffer <= 0) return null
        val track = AudioTrack.Builder()
            .setAudioAttributes(speechAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, MIN_TRACK_BUFFER_BYTES))
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return null
        }
        return track
    }

    private fun releaseTrack() {
        val t = track ?: return
        track = null
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                t.pause()
            }
            t.flush()
            t.stop()
        } catch (_: IllegalStateException) {
            // Track already stopped/released.
        } finally {
            t.release()
        }
    }

    private fun requestAudioFocus() {
        val request = focusRequest ?: AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
            .setAudioAttributes(speechAttributes)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { focusRequest = it }
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    // ------------------------------------------------------------------
    // Main-thread notification
    // ------------------------------------------------------------------

    private fun postReady() {
        mainHandler.post {
            listener?.onReady()
        }
    }

    private fun postDone() {
        mainHandler.post {
            listener?.onDone()
        }
    }

    private fun postError() {
        mainHandler.post {
            listener?.onError()
        }
    }

    interface Listener {
        fun onReady() = Unit
        fun onDone() = Unit
        fun onError() = Unit
    }

    private companion object {
        const val TAG = "AudioModule"
        const val NUM_THREADS = 2
        const val MIN_TRACK_BUFFER_BYTES = 16_384
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 3.0f
        const val DRAIN_POLL_MS = 30L
        const val MAX_DRAIN_WAIT_MS = 15_000L
    }
}
