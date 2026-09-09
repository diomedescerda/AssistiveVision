package com.unimagdalena.assistivevision.model

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShutterKeyPolicyTest {

    @Test
    fun `all remote shutter keycodes are recognized`() {
        assertTrue(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertTrue(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_CAMERA))
        assertTrue(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_HEADSETHOOK))
        assertTrue(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_ENTER))
    }

    @Test
    fun `non-shutter keycodes are rejected`() {
        assertFalse(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_BACK))
        assertFalse(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_MENU))
        assertFalse(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_VOLUME_MUTE))
        assertFalse(ShutterKeyPolicy.isShutterKey(KeyEvent.KEYCODE_SPACE))
        assertFalse(ShutterKeyPolicy.isShutterKey(-1))
    }
}
