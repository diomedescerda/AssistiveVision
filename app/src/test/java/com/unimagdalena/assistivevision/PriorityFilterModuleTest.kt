package com.unimagdalena.assistivevision

import android.graphics.RectF
import com.unimagdalena.assistivevision.model.Detection
import com.unimagdalena.assistivevision.modules.PriorityFilterModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityFilterModuleTest {

    private val filter = PriorityFilterModule()

    private fun detection(classId: Int = 62, zone: String = "center_bottom", score: Float = 0.9f): Detection =
        Detection(
            classId = classId,
            className = "x",
            score = score,
            bbox = RectF(0f, 0f, 10f, 10f),
            zone = zone
        )

    @Test
    fun announces_first_detection_immediately() {
        assertTrue(filter.shouldAnnounce(listOf(detection()), cooldownMs = 2_000L, now = 1_000L))
    }

    @Test
    fun suppresses_repeat_before_cooldown_elapses() {
        val top = listOf(detection())
        assertTrue(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_000L))
        assertFalse(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_500L))
    }

    @Test
    fun announces_again_after_cooldown_elapses() {
        val top = listOf(detection())
        assertTrue(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_000L))
        assertTrue(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 3_500L))
    }

    @Test
    fun announces_immediately_when_top_changes() {
        assertTrue(filter.shouldAnnounce(listOf(detection(classId = 62)), cooldownMs = 2_000L, now = 1_000L))
        assertTrue(filter.shouldAnnounce(listOf(detection(classId = 63)), cooldownMs = 2_000L, now = 1_200L))
    }

    @Test
    fun announces_immediately_when_zone_changes() {
        assertTrue(filter.shouldAnnounce(listOf(detection(zone = "center_bottom")), cooldownMs = 2_000L, now = 1_000L))
        assertTrue(filter.shouldAnnounce(listOf(detection(zone = "right_top")), cooldownMs = 2_000L, now = 1_200L))
    }

    @Test
    fun never_announces_empty_detections() {
        assertFalse(filter.shouldAnnounce(emptyList(), cooldownMs = 2_000L, now = 1_000L))
    }

    @Test
    fun reset_allows_immediate_announcement_of_previous_top() {
        val top = listOf(detection())
        assertTrue(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_000L))
        assertFalse(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_500L))
        filter.reset()
        assertTrue(filter.shouldAnnounce(top, cooldownMs = 2_000L, now = 1_500L))
    }
}