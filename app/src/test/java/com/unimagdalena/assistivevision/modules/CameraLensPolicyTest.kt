package com.unimagdalena.assistivevision.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraLensPolicyTest {

    @Test
    fun `prefers ultra-wide when its focal length is lowest`() {
        val winner = CameraLensPolicy.pick(
            listOf(
                "main" to floatArrayOf(5.6f),
                "ultrawide" to floatArrayOf(2.2f)
            )
        )
        assertEquals("ultrawide", winner)
    }

    @Test
    fun `falls back to main lens when no ultra-wide exists`() {
        val winner = CameraLensPolicy.pick(
            listOf(
                "main" to floatArrayOf(5.6f),
                "telephoto" to floatArrayOf(15.0f)
            )
        )
        assertEquals("main", winner)
    }

    @Test
    fun `uses the lowest focal length within a multi-focal camera`() {
        val winner = CameraLensPolicy.pick(
            listOf(
                "logicalMain" to floatArrayOf(5.6f, 12.0f),
                "logicalMulti" to floatArrayOf(6.0f, 2.5f)
            )
        )
        assertEquals("logicalMulti", winner)
    }

    @Test
    fun `skips candidates without focal length data`() {
        val winner = CameraLensPolicy.pick(
            listOf(
                "noData" to floatArrayOf(),
                "main" to floatArrayOf(5.6f)
            )
        )
        assertEquals("main", winner)
    }

    @Test
    fun `returns null when no candidate has data`() {
        assertNull(CameraLensPolicy.pick(listOf("a" to floatArrayOf(), "b" to floatArrayOf())))
        assertNull(CameraLensPolicy.pick(emptyList()))
    }
}
