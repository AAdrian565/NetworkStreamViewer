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
    }

    @Test
    fun commandsAreUnavailableAfterReceiverCleanup() {
        NdiNative.stopReceiver()
        assertEquals(1, NdiNative.recallPtzPreset(1, 1.0f))
        assertEquals(1, NdiNative.storePtzPreset(1))
        assertEquals(1, NdiNative.stopPtzMovement())

        NdiNative.stopReceiver()
        assertEquals(1, NdiNative.stopPtzMovement())
    }
}
