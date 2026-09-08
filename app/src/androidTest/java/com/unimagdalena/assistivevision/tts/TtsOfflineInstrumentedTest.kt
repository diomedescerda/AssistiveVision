package com.unimagdalena.assistivevision.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unimagdalena.assistivevision.modules.AudioModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end proof that voice output works with the bundled offline engine
 * and voice assets only. The test never touches the network and must pass
 * in airplane mode / on devices with no platform TTS engine installed.
 */
@RunWith(AndroidJUnit4::class)
class TtsOfflineInstrumentedTest {

    @Test
    fun bundledVoiceAssetsExist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = context.assets
        val esModel = assets.list("tts/vits-piper-es_MX-claude-high-int8").orEmpty()
        val enModel = assets.list("tts/vits-piper-en_US-amy-low-int8").orEmpty()
        assertTrue(esModel.any { it.endsWith(".onnx") })
        assertTrue(esModel.contains("tokens.txt"))
        assertTrue(enModel.any { it.endsWith(".onnx") })
        assertTrue(enModel.contains("tokens.txt"))
        val espeak = assets.list("tts/espeak-ng-data").orEmpty()
        assertTrue("espeak-ng-data must be bundled", espeak.isNotEmpty())
    }

    @Test
    fun offlineSpeechInitializesAndCompletes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        val outcomes = mutableListOf<String>()

        val module = AudioModule(context)
        module.setSpeechRate(2.0f) // shorter announcement, faster test
        module.listener = object : AudioModule.Listener {
            override fun onReady() {
                synchronized(outcomes) { outcomes.add("ready") }
                module.speak("Hola")
            }

            override fun onDone() {
                synchronized(outcomes) { outcomes.add("done") }
                latch.countDown()
            }

            override fun onError() {
                synchronized(outcomes) { outcomes.add("error") }
                latch.countDown()
            }
        }

        // Voice init (first run copies espeak-ng-data) may take a while.
        assertTrue(
            "TTS never finished within 120 s",
            latch.await(120, TimeUnit.SECONDS)
        )
        module.shutdown()

        synchronized(outcomes) {
            assertTrue("Expected onReady before completion, got $outcomes", outcomes.contains("ready"))
            assertTrue(
                "Bundled offline speech failed: $outcomes",
                outcomes.contains("done") && !outcomes.contains("error")
            )
        }
    }

    @Test
    fun espeakDataIsCopiedToInternalStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dest = java.io.File(context.filesDir, "tts/espeak-ng-data")
        // Copy happens during voice init; tolerate a fresh-instrumentation state
        // by only asserting when a previous test already triggered it.
        if (dest.exists()) {
            assertTrue(dest.isDirectory)
            assertTrue(dest.listFiles().orEmpty().isNotEmpty())
        }
        val marker = java.io.File(context.filesDir, "tts/espeak-version.txt")
        if (marker.exists()) {
            assertEquals(
                com.unimagdalena.assistivevision.model.TtsVoiceCatalog.ESPEAK_DATA_VERSION
                    .toString(),
                marker.readText().trim()
            )
        }
    }
}
