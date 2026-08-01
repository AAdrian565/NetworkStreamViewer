package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface

internal object NdiNative {
    init {
        System.loadLibrary("ndi_bridge")
    }

    external fun initialize(): Boolean
    external fun discoverSources(timeoutMs: Int): Array<String>
    external fun startReceiver(
        name: String,
        url: String,
        surface: Surface,
        aspectRatioListener: VideoAspectRatioListener
    ): Boolean
    external fun stopReceiver()
    external fun shutdown()
}

fun interface VideoAspectRatioListener {
    fun onVideoAspectRatioChanged(aspectRatio: Float)
}
