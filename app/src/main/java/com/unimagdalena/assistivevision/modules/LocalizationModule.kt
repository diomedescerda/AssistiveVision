package com.unimagdalena.assistivevision.modules

import com.unimagdalena.assistivevision.model.Detection

class LocalizationModule {

    companion object {
        const val LEFT_THRESHOLD = 0.35f
        const val RIGHT_THRESHOLD = 0.65f
        const val BOTTOM_THRESHOLD = 0.5f
    }

    fun localize(detections: List<Detection>, frameWidth: Int, frameHeight: Int): List<Detection> {
        return detections.map { detection ->
            val centerX = (detection.bbox.left + detection.bbox.right) / 2f
            val centerY = (detection.bbox.top + detection.bbox.bottom) / 2f
            val zone = getZone(centerX, centerY, frameWidth, frameHeight)
            detection.copy(zone = zone)
        }
    }

    private fun getZone(centerX: Float, centerY: Float, frameWidth: Int, frameHeight: Int): String {
        val normalizedX = centerX / frameWidth
        val normalizedY = centerY / frameHeight

        val hZone = when {
            normalizedX < LEFT_THRESHOLD -> "left"
            normalizedX > RIGHT_THRESHOLD -> "right"
            else -> "center"
        }

        val vZone = if (normalizedY >= BOTTOM_THRESHOLD) "bottom" else "top"

        return when (hZone) {
            "left" -> "left_$vZone"
            "right" -> "right_$vZone"
            else -> "center_$vZone"
        }
    }
}
