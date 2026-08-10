package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PtzPresetControlsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capabilityChangeShowsAndHidesControls() {
        val supported = mutableStateOf(false)
        composeRule.setContent {
            PtzPresetControls(
                isSupported = supported.value,
                onRecallPreset = {},
                onStorePreset = {},
                onClearPreset = {},
            )
        }

        composeRule.onAllNodesWithContentDescription("PTZ preset controls").assertCountEquals(0)
        composeRule.runOnIdle { supported.value = true }
        composeRule.onNodeWithContentDescription("PTZ preset controls").assertIsDisplayed()
        composeRule.runOnIdle { supported.value = false }
        composeRule.onAllNodesWithContentDescription("PTZ preset controls").assertCountEquals(0)
    }

    @Test
    fun callsByDefaultAndSaveModeStoresOnceThenReturnsToCallMode() {
        var recalledPreset: Int? = null
        var storedPreset: Int? = null
        var clearedPreset: Int? = null
        composeRule.setContent {
            PtzPresetControls(
                isSupported = true,
                onRecallPreset = { recalledPreset = it },
                onStorePreset = { storedPreset = it },
                onClearPreset = { clearedPreset = it },
            )
        }

        composeRule.onNodeWithContentDescription("Call preset 3").performClick()
        composeRule.runOnIdle { assertEquals(3, recalledPreset) }

        composeRule.onNodeWithContentDescription("Save preset mode").performClick()
        composeRule.onNodeWithContentDescription("Save preset 7").performClick()
        composeRule.onNodeWithContentDescription("Confirm overwrite preset 7").performClick()
        composeRule.runOnIdle { assertEquals(7, storedPreset) }

        composeRule.onNodeWithContentDescription("Call preset 8").performClick()
        composeRule.runOnIdle { assertEquals(8, recalledPreset) }

        composeRule.onNodeWithContentDescription("Clear preset mode").performClick()
        composeRule.onNodeWithContentDescription("Clear preset 4").performClick()
        composeRule.runOnIdle { assertEquals(4, clearedPreset) }
        composeRule.onNodeWithContentDescription("Call preset 5").assertIsDisplayed()
    }
}
