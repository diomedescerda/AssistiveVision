package com.unimagdalena.assistivevision.model

/**
 * Builds natural spoken sentences from already-localized text fragments.
 *
 * Input items are (object-with-article, zone-phrase) pairs, e.g.
 * ("una silla", "abajo a la izquierda de donde estás apuntando"), plus the
 * localized connectors. Output example:
 *
 * "Hay una persona donde estás apuntando, también hay una silla abajo a la
 * izquierda de donde estás apuntando y un televisor arriba a la derecha de
 * donde estás apuntando."
 *
 * This class is intentionally free of Android dependencies so it can be
 * unit-tested on the JVM. All localization is resolved before it is called
 * (see [LocalizedLabels]).
 */
object SpeechFormatter {

    /**
     * Builds one natural sentence: the first object is introduced with
     * [thereIs], additional objects with [alsoThereIs], and the last one is
     * joined with [and]. A trailing period is always added.
     */
    fun announce(
        items: List<Pair<String, String>>,
        thereIs: String,
        alsoThereIs: String,
        and: String,
    ): String {
        if (items.isEmpty()) return ""

        val sentence = StringBuilder()
        items.forEachIndexed { index, (name, zone) ->
            val body = if (zone.isNotBlank()) "$name $zone" else name
            when {
                index == 0 -> sentence.append(thereIs.trim()).append(' ').append(body)
                index == items.lastIndex -> sentence.append(' ').append(and.trim()).append(' ').append(body)
                else -> sentence.append(", ").append(alsoThereIs.trim()).append(' ').append(body)
            }
        }
        sentence.append('.')
        return sentence.toString().replaceFirstChar { it.uppercaseChar() }
    }
}
