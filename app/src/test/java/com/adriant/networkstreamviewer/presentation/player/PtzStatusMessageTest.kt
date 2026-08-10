package com.adriant.networkstreamviewer.presentation.player

import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import org.junit.Assert.assertTrue
import org.junit.Test

class PtzStatusMessageTest {
    @Test
    fun reportsEveryCommandOutcomeWithoutAPlaybackError() {
        assertTrue(NdiPtzCommandResult.ACCEPTED.presetStatusMessage("Recall").contains("accepted"))
        assertTrue(
            NdiPtzCommandResult.UNAVAILABLE
                .presetStatusMessage("Recall")
                .contains("unavailable"),
        )
        assertTrue(NdiPtzCommandResult.REJECTED.presetStatusMessage("Recall").contains("rejected"))
        assertTrue(
            NdiPtzCommandResult.INVALID_ARGUMENT
                .presetStatusMessage("Recall")
                .contains("invalid"),
        )
    }
}
