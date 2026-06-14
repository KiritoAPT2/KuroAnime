package com.kuroanime.extractor

class Mp4UploadExtractor : VideoExtractor {
    override val serverKey = "mp4upload"
    override val priority = 20

    override fun canHandle(url: String): Boolean =
        url.contains("mp4upload")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        return extractVideoUrl(html)
    }

    private fun extractVideoUrl(html: String): String? {
        val patterns = listOf(
            Regex("""src:\s*"(https?://[^"]+\.mp4[^"]*)"""),
            Regex("""src:\s*'(https?://[^"]+\.mp4[^']*)'"""),
            Regex("""file:\s*"(https?://[^"]+(?:\.mp4|\.m3u8)[^"]*)"""),
            Regex("""file:\s*'(https?://[^']+(?:\.mp4|\.m3u8)[^']*)'"""),
            Regex("""src:\s*"(//[^"]+\.mp4[^"]*)"""),
            Regex("""<video[^>]+src="(https?://[^"]+)"""),
            Regex("""source\s+src="(https?://[^"]+)"""),
            Regex("""'(https?://[^']+mp4upload[^']+\.mp4)'"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                var url = match.groupValues[1]
                if (url.startsWith("//")) url = "https:$url"
                if (url.isNotBlank()) return url
            }
        }
        return null
    }
}
