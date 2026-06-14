package com.kuroanime.extractor

class StreamtapeExtractor : VideoExtractor {
    override val serverKey = "streamtape"
    override val priority = 10

    override fun canHandle(url: String): Boolean =
        url.contains("streamtape")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null

        val videoSrc = Regex("""<video[^>]+src="((?:https?:)?//[^"]+get_video\?[^"]+)""").find(html)
        if (videoSrc != null) {
            val url = videoSrc.groupValues[1]
            return if (url.startsWith("//")) "https:$url" else url
        }

        for (id in listOf("ideoolink", "botlink", "robotlink")) {
            val el = Regex("""<(?:div|span)[^>]*?id="$id"[^>]*>([^<]+)""").find(html)
            if (el != null) {
                var text = el.groupValues[1].trim()
                if (text.startsWith("//")) text = "https:$text"
                return text
            }
        }

        val base = Regex("""//[^"]*streamtape[^/]+/get_video\?[^"]+""").find(html)
        return base?.let { "https:" + it.value }
    }
}
