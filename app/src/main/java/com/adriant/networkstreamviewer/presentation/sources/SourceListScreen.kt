package com.adriant.networkstreamviewer.presentation.sources

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class NdiMonitorColors(
    val background: Color,
    val topBar: Color,
    val sourceUpper: Color,
    val sourceLower: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val metadataText: Color,
    val metadataLabel: Color,
    val accent: Color,
    val online: Color,
    val offline: Color,
    val countBadge: Color,
    val settingsButton: Color,
)

@Composable
private fun ndiMonitorColors(): NdiMonitorColors {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return NdiMonitorColors(
        background = scheme.background,
        topBar = scheme.surface,
        sourceUpper = scheme.surfaceVariant,
        sourceLower = scheme.surfaceContainerLow,
        border = scheme.outlineVariant,
        primaryText = scheme.onSurface,
        secondaryText = scheme.onSurfaceVariant,
        metadataText = scheme.onSurface,
        metadataLabel = scheme.onSurfaceVariant.copy(alpha = if (isLight) 0.72f else 0.78f),
        accent = scheme.primary,
        online = if (isLight) Color(0xFF16864A) else Color(0xFF45D17C),
        offline = if (isLight) Color(0xFFC63847) else Color(0xFFF15F6B),
        countBadge = if (isLight) Color(0xFFDCEAF7) else Color(0xFF172C4A),
        settingsButton = if (isLight) scheme.surfaceVariant else Color(0xFF181827),
    )
}

/** The deliberately custom-styled source browser shown on the app home screen. */
@Composable
fun NdiMonitorScreen(
    sources: List<NdiSource>,
    status: String,
    permissionGranted: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenCameraSender: () -> Unit,
    onOpenSettings: () -> Unit,
    onSourceSelected: (NdiSource) -> Unit,
) {
    val colors = ndiMonitorColors()
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        NdiTopBar(onOpenSettings = onOpenSettings)
        SourceListHeader(
            count = sources.size,
            isRefreshing = isRefreshing,
            onScan = { if (!isRefreshing) onRefresh() },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!permissionGranted) {
                PermissionPanel(onRequestPermission = onRequestPermission)
            } else {
                val refreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { if (!isRefreshing) onRefresh() },
                    state = refreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        val progress = refreshState.distanceFraction
                        if (!isRefreshing && progress > 0f) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(colors.accent.copy(alpha = 0.18f)),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                                            .fillMaxSize()
                                            .background(colors.accent),
                                )
                            }
                        }
                    },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 76.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (sources.isEmpty()) {
                            item {
                                EmptySourceState(status = status)
                            }
                        }
                        items(
                            items = sources,
                            key = { source -> "${source.name}|${source.url}" },
                        ) { source ->
                            NdiSourceCard(
                                source = source,
                                onClick = { onSourceSelected(source) },
                            )
                        }
                    }
                }
            }

            StreamCameraButton(
                onClick = onOpenCameraSender,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                        .navigationBarsPadding(),
            )
        }
    }
}

/** Kept as the navigation-facing name used by [NdiApp]. */
@Composable
fun SourceListScreen(
    sources: List<NdiSource>,
    status: String,
    permissionGranted: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenCameraSender: () -> Unit,
    onOpenSettings: () -> Unit,
    onSourceSelected: (NdiSource) -> Unit,
) {
    NdiMonitorScreen(
        sources = sources,
        status = status,
        permissionGranted = permissionGranted,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onRequestPermission = onRequestPermission,
        onOpenCameraSender = onOpenCameraSender,
        onOpenSettings = onOpenSettings,
        onSourceSelected = onSourceSelected,
    )
}

@Composable
fun NdiTopBar(onOpenSettings: () -> Unit) {
    val colors = ndiMonitorColors()
    Column(
        modifier = Modifier.background(colors.topBar).windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(8.dp).background(colors.accent, CircleShape),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Network Streams",
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.settingsButton)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable { onOpenSettings() }
                        .semantics { contentDescription = "Settings" },
                contentAlignment = Alignment.Center,
            ) {
                SettingsIcon(color = colors.primaryText)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
fun SourceListHeader(
    count: Int,
    isRefreshing: Boolean = false,
    onScan: () -> Unit,
) {
    val colors = ndiMonitorColors()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "NETWORK STREAMS",
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.size(8.dp))
        Box(
            modifier =
                Modifier
                    .size(width = 22.dp, height = 18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.countBadge),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                color = colors.accent,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = if (isRefreshing) "SCANNING" else "SCAN",
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onScan() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics {
                        contentDescription = "Scan for network streams"
                        role = Role.Button
                    },
        )
    }
}

@Composable
fun NdiSourceCard(
    source: NdiSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(12.dp)
    val contentAlpha = if (source.isOnline) 1f else 0.4f

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(shape)
                .background(colors.sourceUpper)
                .border(1.dp, colors.border, shape)
                .clickable { onClick() },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(colors.sourceUpper)
                    .alpha(contentAlpha)
                    .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(if (source.isOnline) colors.online else colors.offline, CircleShape),
            )
            Spacer(Modifier.size(10.dp))
            val labels = source.monitorLabels()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = labels.hostname,
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = labels.streamName,
                    color = colors.secondaryText,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = labels.ipAddress,
                    color = colors.secondaryText.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = if (source.isOnline) "LIVE" else "OFFLINE",
                color = if (source.isOnline) colors.accent else colors.offline,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.size(12.dp))
            ChevronIcon(color = colors.secondaryText)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(colors.sourceLower)
                    .alpha(contentAlpha)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val details = source.details
            MetadataColumn(
                label = "RESOLUTION",
                value = details?.resolutionText() ?: "—",
                modifier = Modifier.weight(1f),
            )
            MetadataColumn(
                label = "FRAME RATE",
                value = details?.frameRateText() ?: "—",
                modifier = Modifier.weight(1f),
            )
            MetadataColumn(
                label = "FORMAT",
                value = details?.formatText() ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun MetadataColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = colors.metadataLabel,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.35.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            color = colors.metadataText,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StreamCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    Row(
        modifier =
            modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, colors.accent.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .clickable { onClick() }
                .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CameraIcon(color = colors.accent)
        Text(
            text = "Stream Camera to NDI",
            color = colors.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PermissionPanel(onRequestPermission: () -> Unit) {
    val colors = ndiMonitorColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.sourceUpper)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(18.dp),
        ) {
            Text(
                text = "LOCAL NETWORK ACCESS",
                color = colors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Allow access to devices on your local network to discover and play streams.",
                color = colors.primaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ALLOW ACCESS",
                color = colors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRequestPermission() }
                        .padding(vertical = 6.dp)
                        .semantics {
                            contentDescription = "Allow local network access"
                            role = Role.Button
                        },
            )
        }
    }
}

@Composable
private fun EmptySourceState(status: String) {
    val colors = ndiMonitorColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 42.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "NO STREAMS FOUND",
            color = colors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = status,
            color = colors.secondaryText,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SettingsIcon(color: Color) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 1.45.dp.toPx()
        val ringRadius = size.minDimension * 0.27f
        val spokeStart = size.minDimension * 0.36f
        val spokeEnd = size.minDimension * 0.48f
        drawCircle(color = color, radius = size.minDimension * 0.12f, center = center, style = Stroke(strokeWidth))
        drawCircle(color = color, radius = ringRadius, center = center, style = Stroke(strokeWidth))
        repeat(8) { index ->
            val angle = index * PI.toFloat() / 4f
            drawLine(
                color = color,
                start = Offset(center.x + cos(angle) * spokeStart, center.y + sin(angle) * spokeStart),
                end = Offset(center.x + cos(angle) * spokeEnd, center.y + sin(angle) * spokeEnd),
                strokeWidth = strokeWidth,
            )
        }
    }
}

@Composable
private fun ChevronIcon(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val path =
            Path().apply {
                moveTo(3.dp.toPx(), 1.dp.toPx())
                lineTo(8.dp.toPx(), size.height / 2f)
                lineTo(3.dp.toPx(), size.height - 1.dp.toPx())
            }
        drawPath(path = path, color = color, style = Stroke(width = 1.4.dp.toPx()))
    }
}

@Composable
private fun CameraIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(1.dp.toPx(), 4.dp.toPx()),
            size = Size(11.dp.toPx(), 10.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(strokeWidth),
        )
        val lens =
            Path().apply {
                moveTo(12.5.dp.toPx(), 7.dp.toPx())
                lineTo(17.dp.toPx(), 5.dp.toPx())
                lineTo(17.dp.toPx(), 13.dp.toPx())
                lineTo(12.5.dp.toPx(), 11.dp.toPx())
                close()
            }
        drawPath(path = lens, color = color, style = Stroke(strokeWidth))
    }
}

private data class NdiMonitorSourceLabels(
    val hostname: String,
    val streamName: String,
    val ipAddress: String,
)

private fun NdiSource.monitorLabels(): NdiMonitorSourceLabels {
    val nameParts = SOURCE_NAME_PATTERN.matchEntire(name.trim())
    val hostname =
        nameParts
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    val streamName =
        nameParts
            ?.groupValues
            ?.getOrNull(2)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    val urlWithoutScheme = url.substringAfter("://", url)
    val urlHost =
        urlWithoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .trim()
            .takeIf { it.isNotEmpty() }
    return NdiMonitorSourceLabels(
        hostname = hostname ?: urlHost ?: deviceDescription ?: "NDI source",
        streamName = streamName ?: name.ifBlank { "NDI stream" },
        ipAddress = urlHost ?: url.ifBlank { "IP unavailable" },
    )
}

private val SOURCE_NAME_PATTERN = Regex("^(.+?)\\s*\\((.+)\\)$")

private fun NdiStreamDetails.resolutionText(): String = "$width × $height"

private fun NdiStreamDetails.frameRateText(): String {
    val framesPerSecond = frameRateNumerator.toDouble() / frameRateDenominator
    val roundedFramesPerSecond = framesPerSecond.roundToInt()
    return if (abs(framesPerSecond - roundedFramesPerSecond) < 0.005) {
        "$roundedFramesPerSecond fps"
    } else {
        String.format(Locale.US, "%.2f fps", framesPerSecond)
    }
}

private fun NdiStreamDetails.formatText(): String =
    when (format) {
        NdiVideoFormat.FULL_NDI -> "FULL NDI"
        NdiVideoFormat.HX_H264 -> "H.264"
        NdiVideoFormat.HX_HEVC -> "H.265"
    }
