package com.adriant.networkstreamviewer.domain.model

private const val MAX_STREAM_NAME_LENGTH = 64

fun normalizeNdiStreamName(value: String): String =
    value.trim().take(MAX_STREAM_NAME_LENGTH)
