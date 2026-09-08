package com.unimagdalena.assistivevision.modules

/**
 * Pure decision logic for choosing among back-facing cameras.
 *
 * Policy: prefer the camera with the lowest minimum focal length. On phones
 * with an ultra-wide module that is the 0.5x lens; on phones without one the
 * smallest available focal length already belongs to the 1x main lens, so a
 * single rule covers both cases.
 */
object CameraLensPolicy {

    /**
     * @param candidates back-camera ids paired with their available focal lengths (mm).
     * @return the winning camera id, or null when no candidate carries focal
     *         length data (caller should fall back to the default back camera).
     */
    fun pick(candidates: List<Pair<String, FloatArray>>): String? =
        candidates
            .filter { it.second.isNotEmpty() }
            .minByOrNull { it.second.min() }
            ?.first
}
