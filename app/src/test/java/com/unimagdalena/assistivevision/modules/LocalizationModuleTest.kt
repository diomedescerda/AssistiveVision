package com.unimagdalena.assistivevision.modules

import android.graphics.RectF
import com.unimagdalena.assistivevision.model.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests. [RectF] stubs are no-ops under `isReturnDefaultValues`,
 * so boxes are built by assigning its public fields directly.
 */
class LocalizationModuleTest {

    private val localization = LocalizationModule()

    /** Frame 100 wide, 200 tall; places the box center at (x, y). */
    private fun detectionAt(x: Float, y: Float) = Detection(
        classId = 1,
        className = "person",
        score = 0.9f,
        bbox = RectF().apply {
            left = x - 10f
            top = y - 10f
            right = x + 10f
            bottom = y + 10f
        }
    )

    private fun zoneOf(x: Float, y: Float, frameW: Int = 100, frameH: Int = 200) =
        localization.localize(listOf(detectionAt(x, y)), frameW, frameH).single().zone

    @Test
    fun `all six zones are produced`() {
        assertEquals("left_top", zoneOf(34f, 99f))
        assertEquals("left_bottom", zoneOf(34f, 101f))
        assertEquals("center_top", zoneOf(50f, 99f))
        assertEquals("center_bottom", zoneOf(50f, 101f))
        assertEquals("right_top", zoneOf(66f, 99f))
        assertEquals("right_bottom", zoneOf(66f, 101f))
    }

    @Test
    fun `threshold boundaries follow the strict inequality rules`() {
        // x == 0.35 -> NOT left (strict <); x == 0.65 -> NOT right (strict >)
        assertEquals("center_top", zoneOf(35f, 99f))
        assertEquals("center_top", zoneOf(65f, 99f))
        // y == 0.5 -> bottom (>=)
        assertEquals("center_bottom", zoneOf(50f, 100f))
        assertEquals("center_top", zoneOf(50f, 99.9f))
    }

    @Test
    fun `zone assignment preserves the rest of the detection`() {
        val original = detectionAt(50f, 99f)
        val result = localization.localize(listOf(original), 100, 200).single()
        assertEquals(original.classId, result.classId)
        assertEquals(original.className, result.className)
        assertEquals(original.score, result.score, 0f)
        assertEquals("center_top", result.zone)
    }

    @Test
    fun `multiple detections are localized independently`() {
        val result = localization.localize(
            listOf(detectionAt(34f, 99f), detectionAt(66f, 101f)), 100, 200
        )
        assertEquals("left_top", result[0].zone)
        assertEquals("right_bottom", result[1].zone)
    }

    @Test
    fun `degenerate frame size does not divide by zero`() {
        val result = localization.localize(listOf(detectionAt(50f, 99f)), 0, 0)
        assertTrue(result.single().zone.isNotEmpty())
    }

    @Test
    fun `empty input yields empty output`() {
        assertTrue(localization.localize(emptyList(), 100, 200).isEmpty())
    }
}
