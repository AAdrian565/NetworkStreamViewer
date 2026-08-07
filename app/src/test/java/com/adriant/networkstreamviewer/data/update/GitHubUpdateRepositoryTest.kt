package com.adriant.networkstreamviewer.data.update

import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateRepositoryTest {
    @Test
    fun comparesSemanticVersions() {
        assertTrue(compareVersions("1.2.0", "1.1.9") > 0)
        assertTrue(compareVersions("v2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.0", "1.0.0") == 0)
        assertTrue(compareVersions("1.0.1", "1.0.1") == 0)
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
    }
}
