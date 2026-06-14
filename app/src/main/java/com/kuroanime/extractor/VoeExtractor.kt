package com.kuroanime.extractor

class VoeExtractor : VideoExtractor {
    override val serverKey = "voe"
    override val priority = 70

    override fun canHandle(url: String): Boolean =
        url.contains("voe.sx") || url.contains("juliewomanwish")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        val patterns = listOf(
            Regex("""'(https?://[^']+\.m3u8[^']*)'"""),
            Regex("""src="(https?://[^"]+\.m3u8[^"]*)"""),
            Regex("""file:\s*"(https?://[^"]+)"""),
            Regex("""'(https?://[^']+\.mp4[^']*)'"""),
            Regex("""src:\s*'(https?://[^']+)'"""),
        )
        for (pattern in patterns) {
            val m = pattern.find(html)
            if (m != null) {
                val url = m.groupValues[1].replace("\\", "")
                if (url.startsWith("http") && !url.contains("undefined") && !url.contains("null"))
                    return url
            }
        }
        return null
    }
}
