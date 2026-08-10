package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus

@Composable
internal fun AudioControls(
    status: NdiAudioStatus,
    volume: Float,
    muted: Boolean,
    onMutedChanged: (Boolean) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onRetryFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (status == NdiAudioStatus.FOCUS_LOST) {
                        onRetryFocus()
                        onMutedChanged(false)
                    } else {
                        onMutedChanged(!muted)
                    }
                },
                modifier =
                    Modifier.semantics {
                        contentDescription = if (muted) "Unmute audio" else "Mute audio"
                    },
            ) {
                Text(if (muted) "🔇" else "🔊")
            }
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Audio volume" },
            )
        }
        val message =
            when (status) {
                NdiAudioStatus.NO_SIGNAL -> "No audio"
                NdiAudioStatus.FOCUS_LOST -> "Audio focus lost — tap mute to retry"
                NdiAudioStatus.OUTPUT_FAILURE -> "Audio output failed"
                NdiAudioStatus.STARTING, NdiAudioStatus.PLAYING -> null
            }
        message?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
