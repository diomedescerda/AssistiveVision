package com.unimagdalena.assistivevision.model

import android.view.KeyEvent

/**
 * Decides which hardware key events act as the shutter.
 *
 * Bluetooth camera remotes (e.g. a selfie-stick shutter button) pair as HID
 * keyboards and send one of these keycodes depending on the model.
 */
object ShutterKeyPolicy {

    private val SHUTTER_KEYS = intArrayOf(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_ENTER,
    )

    fun isShutterKey(keyCode: Int): Boolean = SHUTTER_KEYS.any { it == keyCode }
}
