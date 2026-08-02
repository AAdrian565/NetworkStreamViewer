package com.adriant.networkstreamviewer.presentation.camera

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.camera.NdiCameraSenderController
import com.adriant.networkstreamviewer.domain.model.CameraSenderSettings
import kotlin.math.max

@Composable
internal fun CameraPreview(
    controller: NdiCameraSenderController,
    settings: CameraSenderSettings,
    bufferWidth: Int,
    bufferHeight: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        key(settings) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    TextureView(context).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) = controller.startCamera(surface, settings)

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) = applyCenterCrop(bufferWidth, bufferHeight)

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                controller.stopCamera()
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                        }
                    }
                },
                update = { view -> view.applyCenterCrop(bufferWidth, bufferHeight) }
            )
        }
    }
}

@Composable
internal fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private fun TextureView.applyCenterCrop(bufferWidth: Int, bufferHeight: Int) {
    if (width == 0 || height == 0 || bufferWidth == 0 || bufferHeight == 0) return
    val scale = max(width.toFloat() / bufferWidth, height.toFloat() / bufferHeight)
    val matrix = Matrix().apply {
        setScale(bufferWidth * scale / width, bufferHeight * scale / height, width / 2f, height / 2f)
    }
    setTransform(matrix)
}
