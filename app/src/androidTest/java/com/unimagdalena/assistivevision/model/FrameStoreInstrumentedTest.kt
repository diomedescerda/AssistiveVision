package com.unimagdalena.assistivevision.model

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regression tests for the camera-frame lifecycle: the original crash was a
 * bitmap recycled by [FrameStore.update] while [FrameStore.copy] was reading
 * it from the main thread.
 */
@RunWith(AndroidJUnit4::class)
class FrameStoreInstrumentedTest {

    @Test
    fun copySurvivesSubsequentUpdateRecyclingTheOriginal() {
        val store = FrameStore()
        val first = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        first.eraseColor(android.graphics.Color.RED)
        store.update(first)

        val copied = store.copy()
        store.update(Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888))

        assertTrue(copied != null)
        assertFalse(copied!!.isRecycled)
        assertEquals(64, copied.width)
        assertEquals(32, copied.height)
        assertEquals(android.graphics.Color.RED, copied.getPixel(0, 0))

        copied.recycle()
        store.clear()
    }

    @Test
    fun concurrentUpdateAndCopyNeverFails() {
        val store = FrameStore()
        store.update(Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888))
        val stop = AtomicBoolean(false)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val copies = AtomicInteger(0)

        val writer = Thread {
            try {
                while (!stop.get()) {
                    store.update(Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888))
                }
            } catch (t: Throwable) {
                failures.add(t)
            }
        }

        val readers = (1..2).map {
            Thread {
                try {
                    while (!stop.get()) {
                        val snapshot = store.copy() ?: continue
                        snapshot.getPixel(0, 0)
                        snapshot.recycle()
                        copies.incrementAndGet()
                    }
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }
        }

        writer.start()
        readers.forEach { it.start() }
        Thread.sleep(1_500)
        stop.set(true)
        writer.join()
        readers.forEach { it.join() }

        assertTrue("FrameStore threw under contention: $failures", failures.isEmpty())
        assertTrue("copy() never produced a bitmap", copies.get() > 0)
        store.clear()
    }
}
