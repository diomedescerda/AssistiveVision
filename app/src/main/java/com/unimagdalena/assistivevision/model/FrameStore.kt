package com.unimagdalena.assistivevision.model

import android.graphics.Bitmap

/**
 * Owns the latest camera frame.
 *
 * Frames arrive on the camera executor while capture happens on the main
 * thread, so [update] and [copy] are serialized on a shared lock: a frame can
 * never be recycled while it is being copied.
 */
class FrameStore {

    private val lock = Any()
    private var frame: Bitmap? = null

    /** Replaces the current frame and recycles the previous one. */
    fun update(bitmap: Bitmap) {
        synchronized(lock) {
            frame?.recycle()
            frame = bitmap
        }
    }

    /**
     * Returns an independent copy of the current frame, or null when no frame
     * is available or the copy fails.
     */
    fun copy(): Bitmap? {
        synchronized(lock) {
            val current = frame ?: return null
            return current.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    /** Drops and recycles the current frame. */
    fun clear() {
        synchronized(lock) {
            frame?.recycle()
            frame = null
        }
    }
}
