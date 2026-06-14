package com.kuroanime.extractor

class OkruExtractor : VideoExtractor {
    override val serverKey = "okru"
    override val priority = 60

    override fun canHandle(url: String): Boolean =
        url.contains("ok.ru")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        val hls = Regex(""""hlsMasterPlaylistUrl":"([^"]+)"""").find(html)
        if (hls != null) return hls.groupValues[1].replace("\\", "")
        val mp4 = Regex(""""mp4":\s*\[.*?"src":"([^"]+)"""", RegexOption.DOT_MATCHES_ALL).find(html)
        if (mp4 != null) return mp4.groupValues[1].replace("\\", "")
        return null
    }
}
