package com.adriant.networkstreamviewer.presentation.sources

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    onSourceSelected: (NdiSource) -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Network streams", style = MaterialTheme.typography.headlineMedium)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.semantics { contentDescription = "Settings" }
                ) {
                    SettingsIcon()
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onOpenCameraSender,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stream this camera to NDI®")
            }
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!permissionGranted) {
                    PermissionCard(onRequestPermission)
                } else {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { if (!isRefreshing) onRefresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SourceList(
                            sources = sources,
                            onSourceSelected = onSourceSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 2.dp.toPx()
        val innerRadius = size.minDimension * 0.16f
        val ringRadius = size.minDimension * 0.30f
        val spokeStart = size.minDimension * 0.34f
        val spokeEnd = size.minDimension * 0.46f

        drawCircle(color = color, radius = innerRadius, center = center, style = Stroke(strokeWidth))
        drawCircle(color = color, radius = ringRadius, center = center, style = Stroke(strokeWidth))
        repeat(8) { index ->
            val angle = index * PI.toFloat() / 4f
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle) * spokeStart,
                    center.y + sin(angle) * spokeStart
                ),
                end = androidx.compose.ui.geometry.Offset(
                    center.x + cos(angle) * spokeEnd,
                    center.y + sin(angle) * spokeEnd
                ),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Allow access to devices on your local network to discover and play streams.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Allow local network")
            }
        }
    }
}

@Composable
private fun SourceList(
    sources: List<NdiSource>,
    onSourceSelected: (NdiSource) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (sources.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No streams found", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Pull down to search again", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(sources, key = { "${it.name}|${it.url}" }) { source ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSourceSelected(source) }
                    .padding(vertical = 18.dp)
            ) {
                Text(source.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        source.details != null -> source.details.subtitle()
                        source.isLoadingDetails -> "Loading stream details…"
                        else -> "Stream details unavailable"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}

private fun NdiStreamDetails.subtitle(): String {
    val framesPerSecond = frameRateNumerator.toDouble() / frameRateDenominator
    val roundedFramesPerSecond = framesPerSecond.roundToInt()
    val frameRateText = if (abs(framesPerSecond - roundedFramesPerSecond) < 0.005) {
        roundedFramesPerSecond.toString()
    } else {
        String.format(Locale.US, "%.2f", framesPerSecond)
    }
    val formatText = when (format) {
        NdiVideoFormat.FULL_NDI -> "Full NDI"
        NdiVideoFormat.HX_H264 -> "NDI HX (H.264)"
        NdiVideoFormat.HX_HEVC -> "NDI HX (HEVC)"
    }
    return "${width}×$height • $frameRateText fps • $formatText"
}
