package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface
import com.adriant.networkstreamviewer.domain.model.NdiSource

class NdiPlayerController {
    fun start(
        source: NdiSource,
        surface: Surface,
        onAspectRatioChanged: (Float) -> Unit
    ): Boolean = NdiNative.startReceiver(
        source.name,
        source.url,
        surface,
        VideoAspectRatioListener(onAspectRatioChanged)
    )

    fun stop() {
        NdiNative.stopReceiver()
    }
}
