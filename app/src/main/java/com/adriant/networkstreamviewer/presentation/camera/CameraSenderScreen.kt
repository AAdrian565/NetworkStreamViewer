package com.adriant.networkstreamviewer.presentation.camera

import android.os.Build
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.camera.NdiCameraSenderController
import com.adriant.networkstreamviewer.domain.model.normalizeNdiStreamName
import com.adriant.networkstreamviewer.domain.model.CameraLens
import com.adriant.networkstreamviewer.domain.model.CameraResolution
import com.adriant.networkstreamviewer.domain.model.CameraSenderSettings
import com.adriant.networkstreamviewer.domain.model.cameraFrameRateOptions
import com.adriant.networkstreamviewer.presentation.rememberCameraPermissionState
import kotlin.math.max

@Composable
fun CameraSenderScreen(
    localNetworkPermissionGranted: Boolean,
    onRequestLocalNetworkPermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermission = rememberCameraPermissionState()
    var streamName by rememberSaveable { mutableStateOf("${Build.MODEL} Camera") }
    var selectedResolution by rememberSaveable { mutableStateOf(CameraResolution.HD) }
    var selectedFrameRate by rememberSaveable { mutableStateOf(30) }
    var selectedLens by rememberSaveable { mutableStateOf(CameraLens.BACK) }
    var cameraReady by remember { mutableStateOf(false) }
    var previewWidth by remember { mutableStateOf(1280) }
    var previewHeight by remember { mutableStateOf(720) }
    var configuredFrameRate by remember { mutableStateOf(30) }
    var isStreaming by remember { mutableStateOf(false) }
    var sentFrameCount by remember { mutableStateOf(0L) }
    var receiverCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val controller = remember {
        NdiCameraSenderController(
            context = context.applicationContext,
            onCameraReadyChanged = { cameraReady = it },
            onCameraConfigured = { width, height, frameRate ->
                previewWidth = width
                previewHeight = height
                configuredFrameRate = frameRate
            },
            onSenderProgress = { frames, receivers ->
                sentFrameCount = frames
                receiverCount = receivers
            },
            onStreamingStopped = { isStreaming = false },
            onError = { errorMessage = it }
        )
    }

    DisposableEffect(controller) {
        onDispose { controller.stopCamera() }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Stream camera to NDI®",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(Modifier.height(16.dp))

            when {
                !localNetworkPermissionGranted -> PermissionCard(
                    message = "Local-network access is required to publish an NDI® stream.",
                    buttonText = "Allow local network",
                    onRequestPermission = onRequestLocalNetworkPermission
                )

                !cameraPermission.isGranted -> PermissionCard(
                    message = "Camera access is required to capture video for the stream.",
                    buttonText = "Allow camera",
                    onRequestPermission = cameraPermission.request
                )

                else -> {
                    val settings = CameraSenderSettings(
                        resolution = selectedResolution,
                        frameRate = selectedFrameRate,
                        lens = selectedLens
                    )
                    CameraPreview(
                        controller = controller,
                        settings = settings,
                        bufferWidth = previewWidth,
                        bufferHeight = previewHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                    Spacer(Modifier.height(16.dp))
                    if (isStreaming) {
                        OutlinedButton(
                            onClick = {
                                controller.stopStreaming()
                                isStreaming = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop streaming")
                        }
                    } else {
                        Text("Camera", style = MaterialTheme.typography.labelLarge)
                        OptionRow {
                            CameraLens.entries.forEach { lens ->
                                FilterChip(
                                    selected = selectedLens == lens,
                                    onClick = { selectedLens = lens },
                                    label = { Text(lens.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Resolution", style = MaterialTheme.typography.labelLarge)
                        OptionRow {
                            CameraResolution.entries.forEach { resolution ->
                                FilterChip(
                                    selected = selectedResolution == resolution,
                                    onClick = { selectedResolution = resolution },
                                    label = { Text(resolution.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Frame rate", style = MaterialTheme.typography.labelLarge)
                        OptionRow {
                            cameraFrameRateOptions.forEach { frameRate ->
                                FilterChip(
                                    selected = selectedFrameRate == frameRate,
                                    onClick = { selectedFrameRate = frameRate },
                                    label = { Text("$frameRate FPS") }
                                )
                            }
                        }
                        Text(
                            text = "Preview: ${previewWidth}×$previewHeight at $configuredFrameRate FPS. " +
                                "The closest camera-supported mode is used.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = streamName,
                            onValueChange = { streamName = it.take(64) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("NDI stream name") }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val normalizedName = normalizeNdiStreamName(streamName)
                                streamName = normalizedName
                                errorMessage = null
                                sentFrameCount = 0
                                receiverCount = 0
                                isStreaming = controller.startStreaming(normalizedName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = cameraReady && streamName.isNotBlank()
                        ) {
                            Text("Start streaming")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            errorMessage != null -> errorMessage.orEmpty()
                            isStreaming && sentFrameCount == 0L ->
                                "Starting the camera stream…"
                            isStreaming && receiverCount == 0 ->
                                "Live — $sentFrameCount frames sent. Waiting for an NDI® receiver. " +
                                    "Keep this screen open and connect from another device."
                            isStreaming ->
                                "Live — $sentFrameCount frames sent to $receiverCount " +
                                    "receiver${if (receiverCount == 1) "" else "s"}. Keep this screen open."
                            cameraReady -> "Camera ready. Choose a name and start streaming."
                            else -> "Starting camera…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    controller: NdiCameraSenderController,
    settings: CameraSenderSettings,
    bufferWidth: Int,
    bufferHeight: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
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
                            ) {
                                controller.startCamera(surface, settings)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                applyCenterCrop(bufferWidth, bufferHeight)
                            }

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
private fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private fun TextureView.applyCenterCrop(bufferWidth: Int, bufferHeight: Int) {
    if (width == 0 || height == 0 || bufferWidth == 0 || bufferHeight == 0) return
    val scale = max(width.toFloat() / bufferWidth, height.toFloat() / bufferHeight)
    val scaledWidth = bufferWidth * scale
    val scaledHeight = bufferHeight * scale
    val matrix = Matrix().apply {
        setScale(
            scaledWidth / width,
            scaledHeight / height,
            width / 2f,
            height / 2f
        )
    }
    setTransform(matrix)
}

@Composable
private fun PermissionCard(
    message: String,
    buttonText: String,
    onRequestPermission: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(message)
            Button(onClick = onRequestPermission) { Text(buttonText) }
        }
    }
}
