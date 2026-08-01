package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
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

    @Test
    fun mapsHxStreamDetails() {
        assertEquals(
            NdiStreamDetails(1920, 1080, 60_000, 1_001, NdiVideoFormat.HX_H264),
            intArrayOf(1920, 1080, 60_000, 1_001, 1).toStreamDetails()
        )
    }

    @Test
    fun rejectsInvalidStreamDetails() {
        assertEquals(null, intArrayOf(1920, 1080, 60).toStreamDetails())
        assertEquals(null, intArrayOf(0, 1080, 60, 1, 0).toStreamDetails())
    }
}
