package com.kuroanime.player

import android.util.Log
import com.kuroanime.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object EmbedResolver {

    private val videoExts = listOf(".mp4", ".m3u8", ".ts", ".webm", ".mkv", ".avi")

    fun needsResolve(url: String): Boolean {
        return videoExts.none { url.contains(it, ignoreCase = true) }
    }

    suspend fun resolve(url: String, referer: String? = null): List<ResolvedVideo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ResolvedVideo>()

        if (!needsResolve(url)) {
            results.add(ResolvedVideo(url, "direct"))
            return@withContext results
        }

        try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
            referer?.let { req.header("Referer", it) }
            val response = HttpClient.client.newCall(req.build()).execute()
            val html = response.body?.string() ?: return@withContext results
            Log.d("EmbedResolver", "Fetched ${url}: ${html.length} chars")

            val ua = Regex("""https?://[^\s"')\]]+\.(?:mp4|m3u8|ts|webm)(?:\?[^\s"')\]]*)?""", RegexOption.IGNORE_CASE)
            results.addAll(ua.findAll(html).map {
                ResolvedVideo(it.value, "regex")
            }.filter { it.url.length < 500 })

            if (results.isEmpty()) {
                val srcRegex = Regex("""src="([^"]+\.(?:mp4|m3u8)[^"]*)"""", RegexOption.IGNORE_CASE)
                results.addAll(srcRegex.findAll(html).map {
                    val u = it.groupValues[1]
                    ResolvedVideo(if (u.startsWith("http")) u else "https:$u", "src")
                })
            }

            if (results.isEmpty()) {
                val dataRegex = Regex("""data-[^=]+="([^"]+\.(?:mp4|m3u8)[^"]*)"""", RegexOption.IGNORE_CASE)
                results.addAll(dataRegex.findAll(html).map {
                    val u = it.groupValues[1]
                    ResolvedVideo(if (u.startsWith("http")) u else "https:$u", "data")
                })
            }

            if (results.isEmpty()) {
                val linkVars = Regex("""(?:link|url|src|videoUrl|file|source)\s*[=:]\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
                results.addAll(linkVars.findAll(html).map {
                    val u = it.groupValues[1]
                    ResolvedVideo(if (u.startsWith("http")) u else "https:$u", "jsvar")
                })
            }

            if (results.isEmpty()) {
                val encodeUri = Regex("""decodeURIComponent\(["']([^"']+)["']\)""", RegexOption.IGNORE_CASE)
                results.addAll(encodeUri.findAll(html).mapNotNull {
                    try {
                        val decoded = java.net.URLDecoder.decode(it.groupValues[1], "UTF-8")
                        ResolvedVideo(decoded, "decodeuri")
                    } catch (_: Exception) { null }
                }.filter { it.url.contains(".mp4") || it.url.contains(".m3u8") })
            }

            if (results.isEmpty()) {
                val inlineText = Regex("""[>]([^<]+\.(?:mp4|m3u8)[^<]*)[<]""", RegexOption.IGNORE_CASE)
                results.addAll(inlineText.findAll(html).map {
                    ResolvedVideo(it.groupValues[1].trim(), "inline")
                }.filter { it.url.startsWith("http") })
            }

            Log.d("EmbedResolver", "Found ${results.size} video URLs")
        } catch (e: Exception) {
            Log.e("EmbedResolver", "Failed to resolve $url: ${e.message}")
        }

        results.distinctBy { it.url }
    }

    data class ResolvedVideo(val url: String, val method: String)
}
