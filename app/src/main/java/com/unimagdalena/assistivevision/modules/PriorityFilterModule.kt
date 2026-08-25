package com.unimagdalena.assistivevision.modules

import com.unimagdalena.assistivevision.model.Detection

/**
 * Decides when a detection should be announced out loud.
 *
 * In automatic mode a new frame arrives several times per second, so without
 * a priority filter the same object would be spoken repeatedly. This module
 * suppresses announcements when the top detection is unchanged and the
 * configured cooldown has not elapsed yet.
 *
 * The module is stateful but small; use [reset] when a new scan/manual
 * capture starts so the next detection is announced immediately.
 */
class PriorityFilterModule {

    private var lastKey: String? = null
    private var lastAnnouncedAt: Long = 0L

    /**
     * @param detections localized detections for the current frame, ordered by score.
     * @param cooldownMs minimum time between repeated announcements of the same top object.
     * @param now current timestamp in millis (injectable for tests).
     *
     * @return true when the caller should speak this frame.
     */
    fun shouldAnnounce(
        detections: List<Detection>,
        cooldownMs: Long,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val top = detections.firstOrNull() ?: return false
        val key = topKey(top)

        val shouldSpeak = key != lastKey || (now - lastAnnouncedAt) >= cooldownMs
        if (shouldSpeak) {
            lastKey = key
            lastAnnouncedAt = now
        }
        return shouldSpeak
    }

    /** Clears the filter so the next detection is announced immediately. */
    fun reset() {
        lastKey = null
        lastAnnouncedAt = 0L
    }

    private fun topKey(top: Detection): String = "${top.classId}:${top.zone}"
}