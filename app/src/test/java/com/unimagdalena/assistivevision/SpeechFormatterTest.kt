package com.unimagdalena.assistivevision

import com.unimagdalena.assistivevision.model.SpeechFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechFormatterTest {

    @Test
    fun single_detection_yields_name_zone() {
        assertEquals("silla, frente a ti", SpeechFormatter.announce("silla", "frente a ti"))
    }

    @Test
    fun multiple_detections_are_joined_with_semicolon() {
        val items = listOf(
            "persona" to "a la derecha",
            "cama" to "abajo a la izquierda"
        )
        assertEquals(
            "persona, a la derecha; cama, abajo a la izquierda",
            SpeechFormatter.announce(items)
        )
    }

    @Test
    fun empty_list_yields_empty_utterance() {
        assertEquals("", SpeechFormatter.announce(emptyList()))
    }

    @Test
    fun single_item_list_matches_single_form() {
        val items = listOf("mesa" to "al centro")
        assertEquals("mesa, al centro", SpeechFormatter.announce(items))
    }
}