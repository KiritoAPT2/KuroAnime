package com.kuroanime.extractor

class MixDropExtractor : VideoExtractor {
    override val serverKey = "mixdrop"
    override val priority = 80

    override fun canHandle(url: String): Boolean =
        url.contains("mixdrop")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        val patterns = listOf(
            Regex("""https?://[^\s"')\]]+\.(?:mp4|m3u8)(?:\?[^\s"')\]]*)?""", RegexOption.IGNORE_CASE),
            Regex("""src="([^"]+\.(?:mp4|m3u8)[^"]*)""""),
            Regex("""file:\s*["']?(https?://[^"'\s>]+\.mp4[^"'\s>]*)"""),
        )
        for (pattern in patterns) {
            val m = pattern.find(html)
            if (m != null) {
                val url = if (m.groupValues.size > 1) m.groupValues[1].replace("\\", "") else m.value
                if (url.startsWith("http") && !url.contains("undefined") && !url.contains("null"))
                    return url
            }
        }
        return null
    }
}
