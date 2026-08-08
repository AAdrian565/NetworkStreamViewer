package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import org.junit.Assert.assertEquals
import org.junit.Test

class NdiPlayerMappingTest {
    @Test
    fun mapsBandwidthToStableNativeValues() {
        assertEquals(0, NdiBandwidth.AUTOMATIC.toNativeValue())
        assertEquals(1, NdiBandwidth.HIGHEST.toNativeValue())
        assertEquals(2, NdiBandwidth.LOWEST.toNativeValue())
    }

    @Test
    fun mapsEveryNativePlaybackState() {
        assertEquals(NdiPlaybackState.CONNECTING, 0.toPlaybackState())
        assertEquals(NdiPlaybackState.WAITING_FOR_KEYFRAME, 1.toPlaybackState())
        assertEquals(NdiPlaybackState.PLAYING, 2.toPlaybackState())
        assertEquals(NdiPlaybackState.DISCONNECTED, 3.toPlaybackState())
        assertEquals(NdiPlaybackState.UNSUPPORTED_CODEC, 4.toPlaybackState())
        assertEquals(NdiPlaybackState.DECODER_FAILURE, 5.toPlaybackState())
        assertEquals(NdiPlaybackState.INSUFFICIENT_BANDWIDTH, 6.toPlaybackState())
        assertEquals(NdiPlaybackState.DECODER_FAILURE, 99.toPlaybackState())
    }

    @Test
    fun validatesPtzPresetAndSpeedRanges() {
        assertEquals(false, isValidPtzPreset(-1))
        assertEquals(true, isValidPtzPreset(0))
        assertEquals(true, isValidPtzPreset(99))
        assertEquals(false, isValidPtzPreset(100))

        assertEquals(false, isValidPtzSpeed(-0.01f))
        assertEquals(true, isValidPtzSpeed(0.0f))
        assertEquals(true, isValidPtzSpeed(1.0f))
        assertEquals(false, isValidPtzSpeed(1.01f))
        assertEquals(false, isValidPtzSpeed(Float.NaN))
        assertEquals(false, isValidPtzSpeed(Float.POSITIVE_INFINITY))
    }

    @Test
    fun validatesMovementAndAbsolutePtzRanges() {
        assertEquals(true, isValidPtzMovementSpeed(-1.0f))
        assertEquals(true, isValidPtzMovementSpeed(1.0f))
        assertEquals(false, isValidPtzMovementSpeed(1.01f))
        assertEquals(false, isValidPtzMovementSpeed(Float.NaN))

        assertEquals(true, isValidPtzAbsoluteValue(0.0f))
        assertEquals(true, isValidPtzAbsoluteValue(1.0f))
        assertEquals(false, isValidPtzAbsoluteValue(-0.01f))
        assertEquals(false, isValidPtzAbsoluteValue(Float.POSITIVE_INFINITY))
    }

    @Test
    fun mapsEveryNativePtzCommandResult() {
        assertEquals(NdiPtzCommandResult.ACCEPTED, 0.toPtzCommandResult())
        assertEquals(NdiPtzCommandResult.UNAVAILABLE, 1.toPtzCommandResult())
        assertEquals(NdiPtzCommandResult.REJECTED, 2.toPtzCommandResult())
        assertEquals(NdiPtzCommandResult.INVALID_ARGUMENT, 3.toPtzCommandResult())
        assertEquals(NdiPtzCommandResult.REJECTED, 99.toPtzCommandResult())
    }
}
