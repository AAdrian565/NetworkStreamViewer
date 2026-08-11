package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import org.junit.Rule
import org.junit.Test

class PlayerControlIconsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playbackIconButtonHasAccessibleLabelInsteadOfAbbreviation() {
        composeRule.setContent {
            PlaybackControlPanel(
                isVisible = true,
                bandwidth = NdiBandwidth.AUTOMATIC,
                audioStatus = NdiAudioStatus.PLAYING,
                audioVolume = 1f,
                audioMuted = false,
                showAudioDbLabels = true,
                onBandwidthChanged = {},
                onAudioMutedChanged = {},
                onAudioVolumeChanged = {},
                onShowAudioDbLabelsChanged = {},
                onRetryAudioFocus = {},
                onClose = {},
            )
        }

        composeRule.onNodeWithContentDescription("Hide playback controls").assertIsDisplayed()
        composeRule.onAllNodesWithText("AV").assertCountEquals(0)
    }

    @Test
    fun ptzIconButtonHasAccessibleLabelInsteadOfAbbreviation() {
        composeRule.setContent {
            PtzControlPanel(
                isSupported = true,
                onPanTiltSpeed = { _, _ -> },
                onZoomSpeed = {},
                onFocusSpeed = {},
                onStop = {},
                onAutoFocus = {},
                onToggleVisibility = {},
                onRecallPreset = {},
                onStorePreset = {},
                onClearPreset = {},
            )
        }

        composeRule.onNodeWithContentDescription("Hide PTZ controls").assertIsDisplayed()
        composeRule.onAllNodesWithText("PTZ").assertCountEquals(0)
    }
}
