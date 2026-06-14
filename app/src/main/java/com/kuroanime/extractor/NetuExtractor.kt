package com.kuroanime.extractor

import com.kuroanime.data.model.QualityOption

class NetuExtractor : VideoExtractor {
    override val serverKey = "netu"
    override val priority = 30

    override fun canHandle(url: String): Boolean =
        url.contains("hqq") || url.contains("netu")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val mirrors = mutableListOf(embedUrl)
        if (embedUrl.contains("hqq.tv")) mirrors.add(embedUrl.replace("hqq.tv", "hqq.net"))
        if (embedUrl.contains("netu.tv") || embedUrl.contains("netu.ac"))
            mirrors.add(embedUrl.replace(Regex("netu\\.(tv|ac)"), "hqq.tv"))

        val patterns = listOf(
            Regex("""file:\s*["']?(https?://[^"'<>\s]+\.m3u8[^"'<>\s]*)"""),
            Regex("""source\s*[=:]\s*["']?(https?://[^"'<>\s]+\.m3u8[^"'<>\s]*)"""),
            Regex("""url:\s*["'](https?://[^"']+\.m3u8[^"']*)"""),
            Regex("""'(https?://[^']+\.m3u8[^']*)'"""),
            Regex("""file:\s*["']?(https?://[^"'<>\s]+\.mp4[^"'<>\s]*)"""),
        )

        for (mirrorUrl in mirrors) {
            val html = HtmlFetcher.fetch(mirrorUrl, referer) ?: continue
            for (pattern in patterns) {
                val m = pattern.find(html)
                if (m != null) {
                    val url = m.groupValues[1].replace("\\", "")
                    if (url.startsWith("http") && !url.contains("undefined") && !url.contains("null")) {
                        return url
                    }
                }
            }
        }
        return null
    }

    suspend fun extractWithQualities(embedUrl: String, referer: String, headers: Map<String, String>? = null): Pair<String?, List<QualityOption>> {
        val videoUrl = extract(embedUrl, referer, headers) ?: return null to emptyList()
        if (!videoUrl.contains(".m3u8")) return videoUrl to emptyList()
        val manifest = HtmlFetcher.fetch(videoUrl, referer) ?: return videoUrl to emptyList()
        val qualities = M3u8Parser.parse(manifest, videoUrl)
        return videoUrl to qualities
    }
}
