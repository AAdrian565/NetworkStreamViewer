package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceHolder
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiSource

internal fun playerSurfaceCallback(
    source: NdiSource,
    bandwidth: NdiBandwidth,
    playerController: NdiPlayerController,
    onAspectRatioChanged: (Float) -> Unit,
    onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
    onPtzSupportChanged: (Boolean) -> Unit
) = object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        onPlaybackStateChanged(NdiPlaybackState.CONNECTING)
        onPtzSupportChanged(false)
        if (!playerController.start(
                source = source,
                surface = holder.surface,
                bandwidth = bandwidth,
                onAspectRatioChanged = onAspectRatioChanged,
                onPlaybackStateChanged = onPlaybackStateChanged,
                onPtzSupportChanged = onPtzSupportChanged
            )) {
            onPlaybackStateChanged(NdiPlaybackState.DECODER_FAILURE)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        onPtzSupportChanged(false)
        playerController.stop()
    }
}
