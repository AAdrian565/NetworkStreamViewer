package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
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
}
