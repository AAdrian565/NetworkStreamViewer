package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiSource

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun PlayerScreen(
    source: NdiSource,
    playerController: NdiPlayerController,
    onBack: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    var videoAspectRatio by remember(source) {
        mutableFloatStateOf(DEFAULT_VIDEO_ASPECT_RATIO)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.aspectRatio(videoAspectRatio),
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(
                        playerSurfaceCallback(
                            source = source,
                            playerController = playerController,
                            onAspectRatioChanged = { ratio ->
                                post { videoAspectRatio = ratio }
                            }
                        )
                    )
                }
            }
        )

        SmallFloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics { contentDescription = "Back to source list" }
        ) {
            CenteredBackArrow()
        }
    }

    DisposableEffect(source) {
        onDispose { playerController.stop() }
    }
}

@Composable
private fun CenteredBackArrow() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val arrow = Path().apply {
            moveTo(size.width * 0.70f, size.height * 0.20f)
            lineTo(size.width * 0.30f, size.height * 0.50f)
            lineTo(size.width * 0.70f, size.height * 0.80f)
        }
        drawPath(
            path = arrow,
            color = color,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun playerSurfaceCallback(
    source: NdiSource,
    playerController: NdiPlayerController,
    onAspectRatioChanged: (Float) -> Unit
) = object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        playerController.start(source, holder.surface, onAspectRatioChanged)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playerController.stop()
    }
}
