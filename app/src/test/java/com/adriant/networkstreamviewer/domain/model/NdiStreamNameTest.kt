package com.adriant.networkstreamviewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NdiStreamNameTest {
    @Test
    fun normalizeTrimsWhitespace() {
        assertEquals("Studio Camera", normalizeNdiStreamName("  Studio Camera  "))
    }

    @Test
    fun normalizeLimitsNameTo64Characters() {
        assertEquals(64, normalizeNdiStreamName("a".repeat(80)).length)
    }
}
