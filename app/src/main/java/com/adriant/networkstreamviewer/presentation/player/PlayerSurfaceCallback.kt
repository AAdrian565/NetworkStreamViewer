package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceHolder
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiAudioDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiSource

internal fun playerSurfaceCallback(
    source: NdiSource,
    bandwidth: NdiBandwidth,
    playerController: NdiPlayerController,
    onAspectRatioChanged: (Float) -> Unit,
    onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
    onPtzSupportChanged: (Boolean) -> Unit,
    initialAudioVolume: Float,
    initialAudioMuted: Boolean,
    onAudioStatusChanged: (NdiAudioStatus) -> Unit,
    onAudioLevelsChanged: (NdiAudioLevels) -> Unit,
    onAudioDiagnosticsChanged: (NdiAudioDiagnostics) -> Unit,
) = object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        onPlaybackStateChanged(NdiPlaybackState.CONNECTING)
        onPtzSupportChanged(false)
        onAudioStatusChanged(NdiAudioStatus.STARTING)
        onAudioLevelsChanged(NdiAudioLevels.FLOOR)
        playerController.start(
            source = source,
            surface = holder.surface,
            bandwidth = bandwidth,
            onAspectRatioChanged = onAspectRatioChanged,
            onPlaybackStateChanged = onPlaybackStateChanged,
            onPtzSupportChanged = onPtzSupportChanged,
            onStartFailed = { onPlaybackStateChanged(NdiPlaybackState.DECODER_FAILURE) },
            initialAudioVolume = initialAudioVolume,
            initialAudioMuted = initialAudioMuted,
            onAudioStatusChanged = onAudioStatusChanged,
            onAudioLevelsChanged = onAudioLevelsChanged,
            onAudioDiagnosticsChanged = onAudioDiagnosticsChanged,
        )
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        onPtzSupportChanged(false)
        onAudioStatusChanged(NdiAudioStatus.STARTING)
        onAudioLevelsChanged(NdiAudioLevels.FLOOR)
        onAudioDiagnosticsChanged(NdiAudioDiagnostics())
        playerController.stopPtzMovement()
        playerController.stop()
    }
}
