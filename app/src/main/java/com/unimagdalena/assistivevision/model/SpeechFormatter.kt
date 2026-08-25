package com.unimagdalena.assistivevision.model

/**
 * Builds spoken utterances from already-localized text fragments.
 *
 * This class is intentionally free of Android dependencies so it can be
 * unit-tested on the JVM. All localization is resolved before it is called
 * (see [LocalizedLabels]).
 */
object SpeechFormatter {

    /**
     * Builds a single announcement for one object, e.g. "silla, frente a ti".
     */
    fun announce(localizedName: String, localizedZone: String): String =
        "$localizedName, $localizedZone"

    /**
     * Builds an announcement for several objects, e.g.
     * "persona, a la derecha; cama, abajo a la izquierda".
     */
    fun announce(detections: List<Pair<String, String>>): String =
        detections.joinToString("; ") { (name, zone) -> announce(name, zone) }
}