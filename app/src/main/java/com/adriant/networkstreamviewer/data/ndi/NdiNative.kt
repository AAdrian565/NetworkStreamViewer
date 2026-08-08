package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface

internal object NdiNative {
    init {
        System.loadLibrary("ndi_bridge")
    }

    external fun initialize(): Boolean
    external fun discoverSources(timeoutMs: Int): Array<String>
    external fun probeSource(name: String, url: String, timeoutMs: Int): IntArray?
    external fun startReceiver(
        name: String,
        url: String,
        surface: Surface,
        bandwidth: Int,
        listener: NdiPlaybackListener
    ): Boolean
    external fun recallPtzPreset(presetNumber: Int, speed: Float): Int
    external fun storePtzPreset(presetNumber: Int): Int
    external fun panTiltSpeed(panSpeed: Float, tiltSpeed: Float): Int
    external fun zoomSpeed(zoomSpeed: Float): Int
    external fun focusSpeed(focusSpeed: Float): Int
    external fun focus(focusValue: Float): Int
    external fun autoFocus(): Int
    external fun whiteBalanceAuto(): Int
    external fun whiteBalanceIndoor(): Int
    external fun whiteBalanceOutdoor(): Int
    external fun whiteBalanceOneShot(): Int
    external fun whiteBalanceManual(red: Float, blue: Float): Int
    external fun stopPtzMovement(): Int
    external fun stopReceiver()
    external fun startSender(name: String): Boolean
    external fun sendVideoFrame(
        nv12Data: ByteArray,
        width: Int,
        height: Int,
        frameRate: Int
    ): Boolean
    external fun senderConnectionCount(): Int
    external fun stopSender()
    external fun shutdown()
}

interface NdiPlaybackListener {
    fun onVideoAspectRatioChanged(aspectRatio: Float)
    fun onPlaybackStateChanged(state: Int)
    fun onPtzSupportChanged(isSupported: Boolean)
}
