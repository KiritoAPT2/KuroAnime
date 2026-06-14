package com.kuroanime.extractor

class YourUploadExtractor : VideoExtractor {
    override val serverKey = "yourupload"
    override val priority = 15

    override fun canHandle(url: String): Boolean =
        url.contains("yourupload")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        val m = Regex("""file:\s*['"]?(https?://[^'"<>\s]+\.mp4[^'"<>\s]*)""").find(html)
        val url = m?.groupValues?.get(1) ?: return null
        if (url.contains("novideo") || url.contains("/embed/")) return null
        return url
    }
}
