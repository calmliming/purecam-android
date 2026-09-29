package com.purecam.app.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashModeTest {
    @Test
    fun cyclesOffAutoOn() {
        assertEquals(FlashMode.AUTO, FlashMode.OFF.next())
        assertEquals(FlashMode.ON, FlashMode.AUTO.next())
        assertEquals(FlashMode.OFF, FlashMode.ON.next())
    }
}
