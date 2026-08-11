package com.adriant.networkstreamviewer.presentation.camera

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.data.camera.NdiCameraSenderController
import com.adriant.networkstreamviewer.domain.model.CameraLens
import com.adriant.networkstreamviewer.domain.model.CameraResolution
import com.adriant.networkstreamviewer.domain.model.CameraSenderSettings
import com.adriant.networkstreamviewer.domain.model.cameraFrameRateOptions
import com.adriant.networkstreamviewer.domain.model.normalizeNdiStreamName
import com.adriant.networkstreamviewer.presentation.rememberCameraPermissionState
import com.adriant.networkstreamviewer.ui.theme.NdiMonitorColors
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors

@Composable
fun CameraSenderScreen(
    localNetworkPermissionGranted: Boolean,
    onRequestLocalNetworkPermission: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val context = androidx.compose.ui.platform.LocalContext.current
    val cameraPermission = rememberCameraPermissionState()
    var streamName by rememberSaveable { mutableStateOf("${Build.MODEL} Camera") }
    var selectedResolution by rememberSaveable { mutableStateOf(CameraResolution.HD) }
    var selectedFrameRate by rememberSaveable { mutableIntStateOf(DEFAULT_FRAME_RATE) }
    var selectedLens by rememberSaveable { mutableStateOf(CameraLens.BACK) }
    var cameraReady by remember { mutableStateOf(false) }
    var previewWidth by remember { mutableIntStateOf(DEFAULT_PREVIEW_WIDTH) }
    var previewHeight by remember { mutableIntStateOf(DEFAULT_PREVIEW_HEIGHT) }
    var configuredFrameRate by remember { mutableIntStateOf(DEFAULT_FRAME_RATE) }
    var isStreaming by remember { mutableStateOf(false) }
    var sentFrameCount by remember { mutableLongStateOf(0L) }
    var receiverCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val controller =
        remember {
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
                onError = { errorMessage = it },
            )
        }

    DisposableEffect(controller) {
        onDispose { controller.stopCamera() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        CameraTopBar(colors = colors, onBack = onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            when {
                !localNetworkPermissionGranted ->
                    PermissionCard(
                        message = "Local-network access is required to publish an NDI® stream.",
                        buttonText = "ALLOW LOCAL NETWORK",
                        onRequestPermission = onRequestLocalNetworkPermission,
                        colors = colors,
                    )

                !cameraPermission.isGranted ->
                    PermissionCard(
                        message = "Camera access is required to capture video for the stream.",
                        buttonText = "ALLOW CAMERA",
                        onRequestPermission = cameraPermission.request,
                        colors = colors,
                    )

                else -> {
                    val settings =
                        CameraSenderSettings(
                            resolution = selectedResolution,
                            frameRate = selectedFrameRate,
                            lens = selectedLens,
                        )
                    CameraSectionHeader(
                        title = "CAMERA PREVIEW",
                        trailing = if (cameraReady) "READY" else "INITIALIZING",
                        colors = colors,
                    )
                    Spacer(Modifier.height(10.dp))
                    PreviewCard(
                        controller = controller,
                        settings = settings,
                        bufferWidth = previewWidth,
                        bufferHeight = previewHeight,
                        previewWidth = previewWidth,
                        previewHeight = previewHeight,
                        frameRate = configuredFrameRate,
                        isReady = cameraReady,
                        colors = colors,
                    )
                    Spacer(Modifier.height(24.dp))

                    if (isStreaming) {
                        StopStreamingButton(
                            onClick = {
                                controller.stopStreaming()
                                isStreaming = false
                            },
                            colors = colors,
                        )
                    } else {
                        CameraSectionHeader(title = "CAMERA SETTINGS", colors = colors)
                        Spacer(Modifier.height(10.dp))
                        CameraSettingsCard(
                            selectedLens = selectedLens,
                            onLensSelected = { selectedLens = it },
                            selectedResolution = selectedResolution,
                            onResolutionSelected = { selectedResolution = it },
                            selectedFrameRate = selectedFrameRate,
                            onFrameRateSelected = { selectedFrameRate = it },
                            previewWidth = previewWidth,
                            previewHeight = previewHeight,
                            frameRate = configuredFrameRate,
                            colors = colors,
                        )
                        Spacer(Modifier.height(24.dp))
                        CameraSectionHeader(title = "STREAM SETTINGS", colors = colors)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "STREAM NAME",
                            color = colors.metadataLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        StreamNameInput(
                            value = streamName,
                            onValueChange = { streamName = it.take(64) },
                            colors = colors,
                        )
                        Spacer(Modifier.height(14.dp))
                        StartStreamingButton(
                            onClick = {
                                val normalizedName = normalizeNdiStreamName(streamName)
                                streamName = normalizedName
                                errorMessage = null
                                sentFrameCount = 0
                                receiverCount = 0
                                isStreaming = controller.startStreaming(normalizedName)
                            },
                            enabled = cameraReady && streamName.isNotBlank(),
                            colors = colors,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    CameraStatus(
                        errorMessage = errorMessage,
                        isStreaming = isStreaming,
                        sentFrameCount = sentFrameCount,
                        receiverCount = receiverCount,
                        cameraReady = cameraReady,
                        colors = colors,
                    )
                }
            }
        }
    }
}

private const val DEFAULT_PREVIEW_WIDTH = 1280
private const val DEFAULT_PREVIEW_HEIGHT = 720
private const val DEFAULT_FRAME_RATE = 30

@Composable
private fun CameraTopBar(
    colors: NdiMonitorColors,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.background(colors.topBar).windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .semantics {
                            contentDescription = "Back"
                            role = Role.Button
                        },
                contentAlignment = Alignment.Center,
            ) {
                BackIcon(color = colors.primaryText)
            }
            Text(
                text = "Stream Camera",
                color = colors.primaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
private fun CameraSectionHeader(
    title: String,
    colors: NdiMonitorColors,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                color = colors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
        }
    }
}

@Composable
private fun PreviewCard(
    controller: NdiCameraSenderController,
    settings: CameraSenderSettings,
    bufferWidth: Int,
    bufferHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    frameRate: Int,
    isReady: Boolean,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .background(colors.sourceUpper)
                .border(1.dp, colors.border, shape),
    ) {
        CameraPreview(
            controller = controller,
            settings = settings,
            bufferWidth = bufferWidth,
            bufferHeight = bufferHeight,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(colors.sourceLower.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier.size(6.dp).background(if (isReady) colors.online else colors.accent, CircleShape),
            )
            Text(
                text = if (isReady) "LIVE PREVIEW" else "PREPARING PREVIEW",
                color = colors.primaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$previewWidth × $previewHeight  •  $frameRate FPS",
                color = colors.secondaryText,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun CameraSettingsCard(
    selectedLens: CameraLens,
    onLensSelected: (CameraLens) -> Unit,
    selectedResolution: CameraResolution,
    onResolutionSelected: (CameraResolution) -> Unit,
    selectedFrameRate: Int,
    onFrameRateSelected: (Int) -> Unit,
    previewWidth: Int,
    previewHeight: Int,
    frameRate: Int,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.sourceUpper.copy(alpha = 0.58f))
                .border(1.dp, colors.border, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OptionLabel(text = "CAMERA", colors = colors)
        OptionRow {
            CameraLens.entries.forEach { lens ->
                NdiOptionButton(
                    text = lens.label,
                    selected = selectedLens == lens,
                    onClick = { onLensSelected(lens) },
                    colors = colors,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OptionLabel(text = "RESOLUTION", colors = colors)
        OptionRow {
            CameraResolution.entries.forEach { resolution ->
                NdiOptionButton(
                    text = resolution.label,
                    selected = selectedResolution == resolution,
                    onClick = { onResolutionSelected(resolution) },
                    colors = colors,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OptionLabel(text = "FRAME RATE", colors = colors)
        OptionRow {
            cameraFrameRateOptions.forEach { option ->
                NdiOptionButton(
                    text = "$option FPS",
                    selected = selectedFrameRate == option,
                    onClick = { onFrameRateSelected(option) },
                    colors = colors,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Preview: $previewWidth × $previewHeight  •  $frameRate FPS",
            color = colors.secondaryText,
            fontSize = 11.sp,
        )
        Text(
            text = "The closest camera-supported mode is used.",
            color = colors.metadataLabel,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun OptionLabel(
    text: String,
    colors: NdiMonitorColors,
) {
    Text(
        text = text,
        color = colors.metadataLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NdiOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(38.dp)
                .clip(shape)
                .background(if (selected) colors.accent.copy(alpha = 0.11f) else Color.Transparent)
                .border(1.dp, if (selected) colors.accent else colors.border, shape)
                .clickable(onClick = onClick)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = if (selected) colors.primaryText else colors.secondaryText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun StreamNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    colors: NdiMonitorColors,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(colors.sourceUpper.copy(alpha = 0.62f))
                .border(1.dp, if (isFocused) colors.accent else colors.border, shape)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 14.dp),
        singleLine = true,
        textStyle =
            TextStyle(
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
        cursorBrush = SolidColor(colors.accent),
        onTextLayout = {},
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun StartStreamingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f))
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CameraIcon(color = if (enabled) colors.background else colors.secondaryText)
        Spacer(Modifier.size(8.dp))
        androidx.compose.material3.Text(
            text = "Start Streaming",
            color = if (enabled) colors.background else colors.secondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StopStreamingButton(
    onClick: () -> Unit,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .border(1.dp, colors.accent.copy(alpha = 0.8f), shape)
                .clickable(onClick = onClick)
                .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "STOP STREAMING",
            color = colors.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun CameraStatus(
    errorMessage: String?,
    isStreaming: Boolean,
    sentFrameCount: Long,
    receiverCount: Int,
    cameraReady: Boolean,
    colors: NdiMonitorColors,
) {
    val status =
        when {
            errorMessage != null -> CameraStatusText("ERROR", errorMessage, colors.offline)
            isStreaming && sentFrameCount == 0L ->
                CameraStatusText("STREAMING", "Starting the camera stream…", colors.accent)
            isStreaming && receiverCount == 0 ->
                CameraStatusText(
                    "LIVE",
                    "Waiting for an NDI® receiver. Keep this screen open and connect from another device.",
                    colors.online,
                )
            isStreaming ->
                CameraStatusText(
                    "LIVE",
                    "$sentFrameCount frames sent to $receiverCount receiver${if (receiverCount == 1) "" else "s"}. " +
                        "Keep this screen open.",
                    colors.online,
                )
            cameraReady ->
                CameraStatusText("CAMERA READY", "Choose a stream name and start broadcasting.", colors.online)
            else -> CameraStatusText("CAMERA INITIALIZING", "Preparing the camera preview…", colors.accent)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.padding(top = 4.dp).size(6.dp).background(status.color, CircleShape),
        )
        Column {
            Text(
                text = status.label,
                color = status.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = status.description,
                color = colors.secondaryText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

private data class CameraStatusText(
    val label: String,
    val description: String,
    val color: Color,
)

@Composable
private fun PermissionCard(
    message: String,
    buttonText: String,
    onRequestPermission: () -> Unit,
    colors: NdiMonitorColors,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.sourceUpper)
                .border(1.dp, colors.border, shape)
                .padding(18.dp),
    ) {
        Text(
            text = "ACCESS REQUIRED",
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            color = colors.primaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = buttonText,
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable(onClick = onRequestPermission)
                    .padding(vertical = 6.dp)
                    .semantics { role = Role.Button },
        )
    }
}

@Composable
private fun BackIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        drawLine(
            color,
            androidx.compose.ui.geometry
                .Offset(14.dp.toPx(), size.height / 2),
            androidx.compose.ui.geometry.Offset(
                4.dp.toPx(),
                size.height / 2,
            ),
            stroke,
        )
        drawLine(
            color,
            androidx.compose.ui.geometry
                .Offset(4.dp.toPx(), size.height / 2),
            androidx.compose.ui.geometry
                .Offset(9.dp.toPx(), 4.dp.toPx()),
            stroke,
        )
        drawLine(
            color,
            androidx.compose.ui.geometry
                .Offset(4.dp.toPx(), size.height / 2),
            androidx.compose.ui.geometry.Offset(
                9.dp.toPx(),
                size.height - 4.dp.toPx(),
            ),
            stroke,
        )
    }
}

@Composable
private fun CameraIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft =
                androidx.compose.ui.geometry
                    .Offset(1.dp.toPx(), 4.dp.toPx()),
            size =
                androidx.compose.ui.geometry
                    .Size(11.dp.toPx(), 10.dp.toPx()),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(2.dp.toPx()),
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(strokeWidth),
        )
        val lens =
            androidx.compose.ui.graphics.Path().apply {
                moveTo(12.5.dp.toPx(), 7.dp.toPx())
                lineTo(17.dp.toPx(), 5.dp.toPx())
                lineTo(17.dp.toPx(), 13.dp.toPx())
                lineTo(12.5.dp.toPx(), 11.dp.toPx())
                close()
            }
        drawPath(
            path = lens,
            color = color,
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(strokeWidth),
        )
    }
}
