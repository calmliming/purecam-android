package com.purecam.app.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomRatioTest {
    @Test
    fun keepsOneDecimal() {
        assertEquals("2.4x", formatZoomRatio(2.4f))
        assertEquals("0.6x", formatZoomRatio(0.6f))
        assertEquals("12.3x", formatZoomRatio(12.34f))
    }

    @Test
    fun dropsTrailingZero() {
        assertEquals("1x", formatZoomRatio(1f))
        assertEquals("2x", formatZoomRatio(2f))
        assertEquals("10x", formatZoomRatio(10f))
    }

    @Test
    fun roundsToNearestTenth() {
        assertEquals("2x", formatZoomRatio(1.96f))
        assertEquals("1.1x", formatZoomRatio(1.06f))
        assertEquals("1x", formatZoomRatio(1.04f))
    }

    @Test
    fun treatsRatiosShownAs1xAsDefault() {
        assertTrue(isDefaultZoom(1f))
        assertTrue(isDefaultZoom(1.04f))
        assertTrue(isDefaultZoom(0.96f))
        assertFalse(isDefaultZoom(1.06f))
        assertFalse(isDefaultZoom(0.94f))
        assertFalse(isDefaultZoom(0.6f))
    }
}
