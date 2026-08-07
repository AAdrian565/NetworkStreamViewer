package com.adriant.networkstreamviewer.domain.repository

import com.adriant.networkstreamviewer.domain.model.AppUpdate

interface UpdateRepository {
    suspend fun findLatestUpdate(currentVersion: String): AppUpdate?

    suspend fun downloadUpdate(update: AppUpdate): String
}
