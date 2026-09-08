package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Text-to-speech wrapper built on the Android platform TTS engine.
 *
 * Responsibilities:
 *  - async engine initialisation with readiness gating,
 *  - applying the app language, speech rate and pitch,
 *  - speaking announcements that interrupt (QUEUE_FLUSH) any stale speech,
 *  - releasing the engine via [shutdown] when the owner is cleared.
 */
class AudioModule(context: Context) {

    private val appContext: Context = context.applicationContext
    private var tts: TextToSpeech? = null

    @Volatile
    private var isReady: Boolean = false
    private var language: String = "es"
    private var speechRate: Float = 1.0f

    /** Optional observer notified when a spoken utterance finishes or fails. */
    var listener: Listener? = null

    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech init failed with status=$status")
            isReady = false
            listener?.onError()
            return@OnInitListener
        }
        val engine = tts ?: return@OnInitListener
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { listener?.onDone() }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { listener?.onDone() }
            @Deprecated("Use onError(String, int)")
            override fun onError(utteranceId: String?) { listener?.onError() }
        })
        isReady = true
        applyLanguage()
        applySpeechRate()
        listener?.onReady()
    }

    init {
        tts = TextToSpeech(appContext, initListener)
    }

    /** Sets the app language tag ("es" or "en") and applies it to the engine. */
    fun setLanguage(language: String) {
        this.language = language
        applyLanguage()
    }

    /** Sets the speech rate multiplier (e.g. 0.5f..2.5f) and applies it. */
    fun setSpeechRate(rate: Float) {
        this.speechRate = rate
        applySpeechRate()
    }

    /** Interrupts current speech and speaks [text] immediately.
     *
     * @return true when the utterance was queued and [Listener.onDone] will
     * eventually fire, false when the engine was not ready (no callback).
     */
    fun speak(text: String): Boolean {
        val engine = tts ?: return false
        if (!isReady) return false
        val utteranceId = "announcement-${System.nanoTime()}"
        engine.stop()
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS
    }

    /** Stops any ongoing speech. */
    fun stop() {
        tts?.stop()
    }

    /** Releases the engine. Must be called when the owner is cleared. */
    fun shutdown() {
        listener = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private fun applyLanguage() {
        val engine = tts ?: return
        if (!isReady) return
        val locale = if (language == "en") Locale.US else Locale("es")
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $locale unavailable ($result); check voice data is installed")
        }
    }

    private fun applySpeechRate() {
        val engine = tts ?: return
        if (!isReady) return
        engine.setSpeechRate(speechRate)
        engine.setPitch(1.0f)
    }

    interface Listener {
        fun onReady() = Unit
        fun onDone() = Unit
        fun onError() = Unit
    }

    private companion object {
        const val TAG = "AudioModule"
    }
}