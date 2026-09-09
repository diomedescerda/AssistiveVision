package com.unimagdalena.assistivevision

import com.unimagdalena.assistivevision.model.SpeechFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechFormatterTest {

    private val thereIs = "Hay"
    private val alsoThereIs = "también hay"
    private val and = "y"

    @Test
    fun single_object_builds_one_natural_sentence() {
        val items = listOf("una persona" to "donde estás apuntando")
        assertEquals(
            "Hay una persona donde estás apuntando.",
            SpeechFormatter.announce(items, thereIs, alsoThereIs, and)
        )
    }

    @Test
    fun two_objects_join_with_and() {
        val items = listOf(
            "una persona" to "donde estás apuntando",
            "una silla" to "a la izquierda de donde estás apuntando"
        )
        assertEquals(
            "Hay una persona donde estás apuntando y una silla a la izquierda de donde estás apuntando.",
            SpeechFormatter.announce(items, thereIs, alsoThereIs, and)
        )
    }

    @Test
    fun three_objects_use_also_and_and() {
        val items = listOf(
            "una persona" to "donde estás apuntando",
            "una silla" to "a la izquierda de donde estás apuntando",
            "una computadora" to "arriba a la derecha de donde estás apuntando"
        )
        assertEquals(
            "Hay una persona donde estás apuntando, también hay una silla a la izquierda de donde estás apuntando y una computadora arriba a la derecha de donde estás apuntando.",
            SpeechFormatter.announce(items, thereIs, alsoThereIs, and)
        )
    }

    @Test
    fun four_objects_repeat_also_for_middle_items() {
        val items = listOf(
            "una persona" to "donde estás apuntando",
            "una silla" to "a la izquierda de donde estás apuntando",
            "una cama" to "a la derecha de donde estás apuntando",
            "una mesa" to "arriba de donde estás apuntando"
        )
        assertEquals(
            "Hay una persona donde estás apuntando, también hay una silla a la izquierda de donde estás apuntando, también hay una cama a la derecha de donde estás apuntando y una mesa arriba de donde estás apuntando.",
            SpeechFormatter.announce(items, thereIs, alsoThereIs, and)
        )
    }

    @Test
    fun empty_list_yields_empty_utterance() {
        assertEquals("", SpeechFormatter.announce(emptyList(), thereIs, alsoThereIs, and))
    }

    @Test
    fun blank_zone_omits_the_zone_phrase() {
        assertEquals("Hay una silla.", SpeechFormatter.announce(listOf("una silla" to ""), thereIs, alsoThereIs, and))
        assertEquals(
            "Hay una persona donde estás apuntando y una silla.",
            SpeechFormatter.announce(
                listOf("una persona" to "donde estás apuntando", "una silla" to ""),
                thereIs, alsoThereIs, and
            )
        )
    }

    @Test
    fun capitalizes_the_first_letter_even_with_lowercase_connectors() {
        val items = listOf("una persona" to "donde estás apuntando")
        assertEquals(
            "Hay una persona donde estás apuntando.",
            SpeechFormatter.announce(items, "hay", alsoThereIs, and)
        )
    }
}
