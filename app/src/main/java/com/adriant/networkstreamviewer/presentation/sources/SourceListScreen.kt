package com.adriant.networkstreamviewer.presentation.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SourceListScreen(
    sources: List<NdiSource>,
    status: String,
    permissionGranted: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenCameraSender: () -> Unit,
    onSourceSelected: (NdiSource) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Network streams", style = MaterialTheme.typography.headlineMedium)
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
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SourceList(
                            sources = sources,
                            onRefresh = onRefresh,
                            onSourceSelected = onSourceSelected
                        )
                    }
                }
            }

            TextButton(onClick = { uriHandler.openUri("https://ndi.video") }) {
                Text("NDI® is a registered trademark of Vizrt NDI AB")
            }
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
    onRefresh: () -> Unit,
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
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefresh) { Text("Refresh") }
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
