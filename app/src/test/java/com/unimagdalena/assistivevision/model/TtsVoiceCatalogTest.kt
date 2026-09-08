package com.unimagdalena.assistivevision.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsVoiceCatalogTest {

    @Test
    fun `spanish voice is bundled`() {
        val spec = TtsVoiceCatalog.specFor("es")
        assertNotNull(spec)
        assertEquals("es", spec!!.languageTag)
        assertTrue(spec.assetDir.startsWith("tts/"))
        assertTrue(spec.modelFile.endsWith(".onnx"))
        assertEquals("tokens.txt", spec.tokensFile)
    }

    @Test
    fun `english voice is bundled`() {
        val spec = TtsVoiceCatalog.specFor("en")
        assertNotNull(spec)
        assertEquals("en", spec!!.languageTag)
        assertTrue(spec.assetDir.startsWith("tts/"))
        assertTrue(spec.modelFile.endsWith(".onnx"))
    }

    @Test
    fun `spanish and english voices are distinct`() {
        val es = TtsVoiceCatalog.specFor("es")!!
        val en = TtsVoiceCatalog.specFor("en")!!
        assertNotEquals(es.assetDir, en.assetDir)
        assertNotEquals(es.modelFile, en.modelFile)
    }

    @Test
    fun `unsupported language has no voice`() {
        assertNull(TtsVoiceCatalog.specFor("fr"))
        assertNull(TtsVoiceCatalog.specFor(""))
        assertNull(TtsVoiceCatalog.specFor("de"))
    }

    @Test
    fun `lookup is case and whitespace insensitive`() {
        assertEquals(TtsVoiceCatalog.specFor("es"), TtsVoiceCatalog.specFor("ES"))
        assertEquals(TtsVoiceCatalog.specFor("en"), TtsVoiceCatalog.specFor(" en "))
    }

    @Test
    fun `default language is spanish`() {
        assertEquals("es", TtsVoiceCatalog.DEFAULT_LANGUAGE)
        assertNotNull(TtsVoiceCatalog.specFor(TtsVoiceCatalog.DEFAULT_LANGUAGE))
    }

    @Test
    fun `every bundled voice resolves to an asset path under the tts root`() {
        for (spec in TtsVoiceCatalog.all()) {
            assertTrue(spec.assetDir.startsWith("tts/"))
            assertEquals(spec.languageTag, TtsVoiceCatalog.specFor(spec.languageTag)?.languageTag)
        }
    }
}
