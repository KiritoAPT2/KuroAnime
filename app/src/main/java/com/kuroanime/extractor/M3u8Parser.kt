package com.kuroanime.extractor

import com.kuroanime.data.model.QualityOption

object M3u8Parser {
    private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+)x(\d+)""")
    private val BANDWIDTH_REGEX = Regex("""BANDWIDTH=(\d+)""")
    private val URI_LINE_REGEX = Regex("""^(?!#)(.+)$""")

    fun parse(manifest: String, baseUrl: String): List<QualityOption> {
        val lines = manifest.lines()
        val qualities = mutableListOf<QualityOption>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val resolution = RESOLUTION_REGEX.find(line)
                val bandwidth = BANDWIDTH_REGEX.find(line)
                val nextLine = lines.getOrNull(i + 1)?.trim()
                if (nextLine != null && !nextLine.startsWith("#")) {
                    val url = resolveUrl(nextLine, baseUrl)
                    val label = resolution?.let {
                        val width = it.groupValues[1]
                        val height = it.groupValues[2]
                        "${height}p"
                    } ?: "Auto"
                    qualities.add(
                        QualityOption(
                            label = label,
                            url = url,
                            bandwidth = bandwidth?.groupValues?.get(1)?.toLongOrNull() ?: 0,
                        )
                    )
                    i++
                }
            }
            i++
        }

        return qualities
    }

    private fun resolveUrl(url: String, baseUrl: String): String {
        if (url.startsWith("http")) return url
        val base = baseUrl.substringBeforeLast("/")
        return if (url.startsWith("/")) {
            val domain = baseUrl.substringBefore("/", baseUrl.substringAfter("://"))
            "$domain$url"
        } else {
            "$base/$url"
        }
    }
}
