package com.adriant.networkstreamviewer.data.ndi

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class NdiNativeAudioInstrumentedTest {
    @Before
    fun setUp() {
        NdiNative.initialize()
    }

    @After
    fun tearDown() {
        NdiNative.stopReceiver()
        NdiNative.shutdown()
    }

    @Test
    fun validatesAudioBufferBoundaryWithoutAReceiver() {
        assertEquals(-2L, NdiNative.fillAudioBuffer(ByteBuffer.allocate(3), 48_000, 2, 960))
        assertEquals(-2L, NdiNative.fillAudioBuffer(ByteBuffer.allocateDirect(3), 48_000, 2, 960))
        assertEquals(-2L, NdiNative.fillAudioBuffer(ByteBuffer.allocateDirect(4), 48_000, 0, 1))
    }

    @Test
    fun stoppedReceiverReturnsNoAudioAndEmptyStats() {
        NdiNative.stopReceiver()
        assertEquals(-1L, NdiNative.fillAudioBuffer(ByteBuffer.allocateDirect(3_840), 48_000, 2, 960))
        assertEquals(0, NdiNative.getAudioPerformance().size)
        NdiNative.stopReceiver()
    }
}
