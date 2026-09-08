package com.unimagdalena.assistivevision.model

/**
 * Declarative catalog of the voice models bundled under `assets/tts/`.
 *
 * The app ships one voice per supported language and requires them at
 * runtime (fully offline; there is no platform-TTS or network fallback).
 * Keeping the mapping here — pure Kotlin, no Android imports — makes it
 * unit-testable and lets a release checklist verify the asset layout.
 */
data class TtsVoiceSpec(
    /** App language tag as stored in settings ("es" / "en"). */
    val languageTag: String,
    /** Asset directory that contains the voice package. */
    val assetDir: String,
    /** Voice ONNX model file name inside [assetDir]. */
    val modelFile: String,
    /** Tokenizer file name inside [assetDir]. */
    val tokensFile: String,
)

object TtsVoiceCatalog {

    /**
     * The espeak-ng phonemization data is byte-identical across the bundled
     * voice packages, so a single shared copy lives under `assets/tts/`.
     */
    const val ESPEAK_DATA_ASSET_DIR = "tts/espeak-ng-data"

    /** Bump when the bundled espeak-ng-data changes; forces a runtime re-copy. */
    const val ESPEAK_DATA_VERSION = 1

    /** New installs default to Spanish. */
    const val DEFAULT_LANGUAGE = "es"

    private val VOICES = mapOf(
        "es" to TtsVoiceSpec(
            languageTag = "es",
            assetDir = "tts/vits-piper-es_MX-claude-high-int8",
            modelFile = "es_MX-claude-high.onnx",
            tokensFile = "tokens.txt",
        ),
        "en" to TtsVoiceSpec(
            languageTag = "en",
            assetDir = "tts/vits-piper-en_US-amy-low-int8",
            modelFile = "en_US-amy-low.onnx",
            tokensFile = "tokens.txt",
        ),
    )

    /** Returns the bundled voice for [languageTag], or null when unsupported. */
    fun specFor(languageTag: String): TtsVoiceSpec? = VOICES[languageTag.trim().lowercase()]

    /** All bundled voices; used by the release checklist and tests. */
    fun all(): Collection<TtsVoiceSpec> = VOICES.values
}
