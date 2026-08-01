package com.adriant.networkstreamviewer.domain.model

data class NdiSource(
    val name: String,
    val url: String,
    val details: NdiStreamDetails? = null
)
