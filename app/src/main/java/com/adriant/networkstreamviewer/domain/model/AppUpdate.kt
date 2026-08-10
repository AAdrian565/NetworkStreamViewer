package com.adriant.networkstreamviewer.domain.model

data class AppUpdate(
    val version: String,
    val downloadUrl: String,
    val releaseUrl: String,
)
