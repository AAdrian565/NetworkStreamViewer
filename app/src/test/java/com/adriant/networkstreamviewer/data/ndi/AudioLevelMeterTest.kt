package com.adriant.networkstreamviewer.data.ndi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioLevelMeterTest {
    @Test
    fun silenceMapsToFloor() {
        val levels = meter().process(block(0, 0), 0)
        assertEquals(-60f, levels.leftPeakDbfs, 0.01f)
        assertEquals(-60f, levels.leftRmsDbfs, 0.01f)
    }

    @Test
    fun fullScaleMapsToZeroDbfs() {
        val levels = meter().process(block(Short.MAX_VALUE.toInt(), Short.MIN_VALUE.toInt()), 0)
        assertEquals(0f, levels.leftPeakDbfs, 0.01f)
        assertEquals(0f, levels.rightPeakDbfs, 0.01f)
        assertTrue(levels.leftClipped && levels.rightClipped)
    }

    @Test
    fun halfScaleMapsToApproximatelyMinusSixDbfs() {
        val value = (Short.MAX_VALUE / 2).toInt()
        val levels = meter().process(block(value, value), 0)
        assertEquals(-6.02f, levels.leftRmsDbfs, 0.03f)
    }

    @Test
    fun channelsAreIndependent() {
        val levels = meter().process(block(Short.MAX_VALUE.toInt(), 0), 0)
        assertEquals(0f, levels.leftPeakDbfs, 0.01f)
        assertEquals(-60f, levels.rightPeakDbfs, 0.01f)
    }

    @Test
    fun rmsUsesTheWholeWaveform() {
        val buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        buffer.asShortBuffer().put(shortArrayOf(16384, 16384, 0, 0))
        val levels = meter().process(buffer, 0)
        assertEquals(-9.03f, levels.leftRmsDbfs, 0.04f)
    }

    @Test
    fun peakHoldAndReleaseAreClocked() {
        val meter = meter()
        meter.process(block(Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()), 0)
        val held = meter.process(block(0, 0), 500)
        assertEquals(0f, held.leftPeakDbfs, 0.01f)
        val released = meter.process(block(0, 0), 1_000)
        assertEquals(-5f, released.leftPeakDbfs, 0.1f)
    }

    @Test
    fun clippingIsHeldForOneSecond() {
        val meter = meter()
        assertTrue(meter.process(block(Short.MAX_VALUE.toInt(), 0), 0).leftClipped)
        assertTrue(meter.process(block(0, 0), 500).leftClipped)
        assertTrue(!meter.process(block(0, 0), 1_001).leftClipped)
    }

    @Test
    fun processingDoesNotChangeBufferPositionOrLimit() {
        val buffer = block(100, -100)
        buffer.position(2)
        val limit = buffer.limit()
        meter().process(buffer, 0)
        assertEquals(2, buffer.position())
        assertEquals(limit, buffer.limit())
    }

    @Test
    fun resetClearsAllState() {
        val meter = meter()
        meter.process(block(Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()), 0)
        val reset = meter.reset()
        assertEquals(-60f, reset.leftPeakDbfs, 0.01f)
        assertEquals(false, reset.leftClipped)
    }

    private fun meter() = AudioLevelMeter(AudioLevelMeter.Clock { 0L })

    private fun block(
        left: Int,
        right: Int,
    ): ByteBuffer =
        ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).apply {
            asShortBuffer().put(shortArrayOf(left.toShort(), right.toShort(), left.toShort(), right.toShort()))
        }
}
