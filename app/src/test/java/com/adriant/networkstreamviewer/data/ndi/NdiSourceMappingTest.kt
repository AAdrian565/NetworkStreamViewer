package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import org.junit.Assert.assertEquals
import org.junit.Test

class NdiSourceMappingTest {
    @Test
    fun mapsAlternatingNamesAndUrls() {
        val rawSources = arrayOf(
            "Studio (Camera 1)", "ndi://studio/camera-1",
            "Laptop (Screen)", "ndi://laptop/screen"
        )

        assertEquals(
            listOf(
                NdiSource("Studio (Camera 1)", "ndi://studio/camera-1"),
                NdiSource("Laptop (Screen)", "ndi://laptop/screen")
            ),
            rawSources.toNdiSources()
        )
    }

    @Test
    fun ignoresIncompleteTrailingValue() {
        assertEquals(emptyList<NdiSource>(), arrayOf("Name without URL").toNdiSources())
    }
}
