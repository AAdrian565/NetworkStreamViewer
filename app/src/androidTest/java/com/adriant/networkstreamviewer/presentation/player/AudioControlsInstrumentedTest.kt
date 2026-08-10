package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioControlsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun muteButtonIsAccessibleAndToggles() {
        var muted = false
        composeRule.setContent {
            AudioControls(
                status = NdiAudioStatus.PLAYING,
                volume = 0.5f,
                muted = muted,
                onMutedChanged = { muted = it },
                onVolumeChanged = {},
                onRetryFocus = {}
            )
        }
        composeRule.onNodeWithContentDescription("Mute audio").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, muted) }
    }
}
