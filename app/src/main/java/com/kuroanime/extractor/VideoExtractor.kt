package com.kuroanime.extractor

interface VideoExtractor {
    val serverKey: String
    val priority: Int

    fun canHandle(url: String): Boolean
    suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String?
}
