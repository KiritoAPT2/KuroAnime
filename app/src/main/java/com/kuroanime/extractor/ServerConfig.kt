package com.kuroanime.extractor

data class ServerConfig(
    val timeoutMs: Long,
    val maxRetries: Int = 0,
    val retryableErrorTypes: List<String> = emptyList(),
)
