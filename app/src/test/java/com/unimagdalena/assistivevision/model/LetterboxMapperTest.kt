package com.unimagdalena.assistivevision.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LetterboxMapperTest {

    private val input = 448

    @Test
    fun `square source needs no padding`() {
        val g = LetterboxMapper.forward(448, 448, input)
        assertEquals(1f, g.scale, 1e-6f)
        assertEquals(448, g.dstWidth)
        assertEquals(448, g.dstHeight)
        assertEquals(0f, g.dx, 1e-6f)
        assertEquals(0f, g.dy, 1e-6f)
    }

    @Test
    fun `portrait source is width-constrained`() {
        val g = LetterboxMapper.forward(720, 1280, input)
        val expectedScale = 448f / 1280f
        assertEquals(expectedScale, g.scale, 1e-6f)
        assertEquals((720 * expectedScale).toInt(), g.dstWidth) // 252
        assertEquals(448, g.dstHeight)
        assertEquals((448 - 252) / 2f, g.dx, 1e-6f) // 98
        assertEquals(0f, g.dy, 1e-6f)
    }

    @Test
    fun `landscape source is height-constrained`() {
        val g = LetterboxMapper.forward(1280, 720, input)
        assertEquals(0f, g.dx, 1e-6f)
        assertEquals((448 - 252) / 2f, g.dy, 1e-6f)
    }

    @Test
    fun `inverse round-trips a frame point through forward geometry`() {
        val frameW = 720
        val frameH = 1280
        val g = LetterboxMapper.forward(frameW, frameH, input)

        // Sample points across the frame
        for (frameX in listOf(0f, 10f, 360f, 719f, 720f)) {
            val modelX = g.dx + frameX * g.scale
            assertEquals(frameX, LetterboxMapper.toFrameX(modelX, g, frameW), 0.01f)
        }
        for (frameY in listOf(0f, 640f, 1279f, 1280f)) {
            val modelY = g.dy + frameY * g.scale
            assertEquals(frameY, LetterboxMapper.toFrameY(modelY, g, frameH), 0.01f)
        }
    }

    @Test
    fun `inverse maps padded model edges back to frame edges`() {
        val frameW = 720
        val frameH = 1280
        val g = LetterboxMapper.forward(frameW, frameH, input)

        assertEquals(0f, LetterboxMapper.toFrameX(g.dx, g, frameW), 0.01f)
        assertEquals(
            frameW.toFloat(),
            LetterboxMapper.toFrameX(g.dx + g.dstWidth, g, frameW),
            0.01f
        )
        assertEquals(0f, LetterboxMapper.toFrameY(g.dy, g, frameH), 0.01f)
        assertEquals(
            frameH.toFloat(),
            LetterboxMapper.toFrameY(g.dy + g.dstHeight, g, frameH),
            0.01f
        )
    }

    @Test
    fun `rejects non-positive dimensions`() {
        assertThrows(IllegalArgumentException::class.java) {
            LetterboxMapper.forward(0, 100, input)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LetterboxMapper.forward(100, 100, 0)
        }
    }
}
