package com.adriant.networkstreamviewer.data.ndi

import org.junit.Assert.assertEquals
import org.junit.Test

class NdiNativePtzInstrumentedTest {
    @Test
    fun validatesCommandRangesBeforeAccessingReceiver() {
        assertEquals(3, NdiNative.recallPtzPreset(-1, 1.0f))
        assertEquals(3, NdiNative.recallPtzPreset(100, 1.0f))
        assertEquals(3, NdiNative.recallPtzPreset(1, -0.01f))
        assertEquals(3, NdiNative.recallPtzPreset(1, 1.01f))
        assertEquals(3, NdiNative.recallPtzPreset(1, Float.NaN))
        assertEquals(3, NdiNative.storePtzPreset(-1))
        assertEquals(3, NdiNative.storePtzPreset(100))
        assertEquals(3, NdiNative.panTiltSpeed(-1.01f, 0.0f))
        assertEquals(3, NdiNative.panTiltSpeed(0.0f, 1.01f))
        assertEquals(3, NdiNative.zoomSpeed(0.51f))
        assertEquals(3, NdiNative.zoomSpeed(Float.NaN))
        assertEquals(3, NdiNative.focusSpeed(-0.51f))
        assertEquals(3, NdiNative.focus(1.01f))
        assertEquals(3, NdiNative.whiteBalanceManual(0.0f, 1.01f))
    }

    @Test
    fun commandsAreUnavailableAfterReceiverCleanup() {
        NdiNative.stopReceiver()
        assertEquals(1, NdiNative.recallPtzPreset(1, 1.0f))
        assertEquals(1, NdiNative.storePtzPreset(1))
        assertEquals(1, NdiNative.panTiltSpeed(0.5f, 0.0f))
        assertEquals(1, NdiNative.zoomSpeed(0.5f))
        assertEquals(1, NdiNative.focusSpeed(0.5f))
        assertEquals(1, NdiNative.focus(0.5f))
        assertEquals(1, NdiNative.autoFocus())
        assertEquals(1, NdiNative.whiteBalanceAuto())
        assertEquals(1, NdiNative.whiteBalanceIndoor())
        assertEquals(1, NdiNative.whiteBalanceOutdoor())
        assertEquals(1, NdiNative.whiteBalanceOneShot())
        assertEquals(1, NdiNative.whiteBalanceManual(0.5f, 0.5f))
        assertEquals(1, NdiNative.stopPtzMovement())

        NdiNative.stopReceiver()
        assertEquals(1, NdiNative.stopPtzMovement())
    }
}
