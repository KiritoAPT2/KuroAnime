package com.kuroanime.extension

import android.util.Log
import com.kuroanime.data.HttpClient
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request

@Serializable
data class TioAnimeSearchResponse(
    val results: List<TioAnimeItem> = emptyList()
)

@Serializable
data class TioAnimeItem(
    val url: String = "",
    val name: String = "",
    val image: String? = null
)

@Serializable
data class TioAnimeDetail(
    val name: String = "",
    val image: String? = null,
    val synopsis: String? = null,
    val episodes: List<TioAnimeEp>? = null
)

@Serializable
data class TioAnimeEp(
    val name: String? = null,
    val url: String = "",
    val num: Int? = null
)

@Serializable
data class TioAnimeServer(
    val name: String? = null,
    val url: String? = null
)

class TioAnimeExtension : AnimeExtension {
    override val name = "TioAnime"
    override val baseUrl = "https://jimov-api.vercel.app"
    override val lang = "es"

    private val apiBase = "$baseUrl/anime/tioanime"
    private val json = Json { ignoreUnknownKeys = true }

    private fun fetchApi(url: String): String? {
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .build()
            val resp = HttpClient.client.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (_: Exception) { null }
    }

    override suspend fun search(query: String): List<Anime> {
        val body = fetchApi("$apiBase/filter?q=${query.replace(" ", "+")}&page=1") ?: return emptyList()
        val items = try {
            json.decodeFromString<List<TioAnimeItem>>(body)
        } catch (_: Exception) {
            json.decodeFromString<TioAnimeSearchResponse>(body).results
        }
        return items.map { Anime(title = it.name, url = it.url, imageUrl = it.image, source = name) }
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        return search(genre)
    }

    override suspend fun getLatest(page: Int): List<Anime> {
        val body = fetchApi("$apiBase/filter?page=$page") ?: return emptyList()
        val items = try {
            json.decodeFromString<List<TioAnimeItem>>(body)
        } catch (_: Exception) {
            json.decodeFromString<TioAnimeSearchResponse>(body).results
        }
        return items.map { Anime(title = it.name, url = it.url, imageUrl = it.image, source = name) }
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        val body = fetchApi(url) ?: return Anime(title = "", url = url, source = name)
        return try {
            val detail = json.decodeFromString<TioAnimeDetail>(body)
            val eps = detail.episodes?.mapNotNull { ep ->
                val num = ep.num ?: ep.name?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                Episode(number = num, title = ep.name ?: "Episodio $num", url = ep.url)
            } ?: emptyList()
            Anime(
                title = detail.name, url = url,
                imageUrl = detail.image,
                synopsis = detail.synopsis,
                episodes = eps, source = name
            )
        } catch (e: Exception) {
            Log.e("TioAnime", "getAnimeInfo failed: ${e.message}")
            Anime(title = "", url = url, source = name)
        }
    }

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"

        val embedServers = try {
            val body = fetchApi(episodeUrl) ?: return emptyList()
            json.decodeFromString<List<TioAnimeServer>>(body)
                .filter { it.name != null && it.url != null }
        } catch (_: Exception) {
            emptyList()
        }

        if (embedServers.isEmpty()) {
            val extracted = extractFromEmbed(episodeUrl, ua)
            if (extracted != null) {
                return listOf(VideoSource(url = extracted, server = name, quality = "API",
                    headers = mapOf("Referer" to refererFor(episodeUrl), "User-Agent" to ua)))
            }
            return listOf(VideoSource(url = episodeUrl, server = name, quality = "API",
                headers = mapOf("Referer" to "https://tioanime.com/", "User-Agent" to ua)))
        }

        val preferred = listOf("YourUpload", "Netu", "HQQ", "Okru", "Mp4Upload", "Streamtape")
        val sorted = preferred.mapNotNull { p -> embedServers.find { it.name == p } } +
            embedServers.filter { it.name !in preferred }

        return sorted.mapNotNull { entry ->
            val embedUrl = entry.url ?: return@mapNotNull null
            val extracted = extractFromEmbed(embedUrl, ua)
            val serverName = entry.name ?: "API"
            if (extracted != null) {
                VideoSource(url = extracted, server = serverName, quality = name,
                    headers = mapOf("Referer" to refererFor(embedUrl), "User-Agent" to ua))
            } else {
                VideoSource(url = embedUrl, server = serverName, quality = name,
                    headers = mapOf("User-Agent" to ua))
            }
        }
    }

    private fun extractFromEmbed(embedUrl: String, ua: String): String? {
        return when {
            embedUrl.contains("yourupload.com") -> extractYourUpload(embedUrl, ua)
            embedUrl.contains("hqq.tv") || embedUrl.contains("netu") -> extractNetu(embedUrl, ua)
            embedUrl.contains("ok.ru") -> extractOkru(embedUrl, ua)
            embedUrl.contains("mp4upload.com") -> extractMp4Upload(embedUrl, ua)
            embedUrl.contains("streamtape.com") -> extractStreamtape(embedUrl, ua)
            else -> null
        }
    }

    private fun fetchHtml(url: String, ua: String, referer: String? = null): String? {
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", ua)
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            referer?.let { req.header("Referer", it) }
            val resp = HttpClient.client.newCall(req.build()).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (_: Exception) { null }
    }

    private fun extractYourUpload(embedUrl: String, ua: String): String? {
        val html = fetchHtml(embedUrl, ua, "https://www.yourupload.com/") ?: return null
        val m = Regex("""file:\s*['"]?(https?://[^'"<>\s]+\.mp4[^'"<>\s]*)""").find(html)
        val url = m?.groupValues?.get(1) ?: return null
        if (url.contains("novideo") || url.contains("/embed/")) return null
        return url
    }

    private fun extractNetu(embedUrl: String, ua: String): String? {
        val mirrors = mutableListOf(embedUrl)
        if (embedUrl.contains("hqq.tv")) {
            mirrors.add(embedUrl.replace("hqq.tv", "hqq.net"))
        }
        if (embedUrl.contains("netu.tv") || embedUrl.contains("netu.ac")) {
            mirrors.add(embedUrl.replace(Regex("netu\\.(tv|ac)"), "hqq.tv"))
        }

        val patterns = listOf(
            Regex("""'(https?://[^']+\.m3u8[^']*)'"""),
            Regex(""""(https?://[^"]+\.m3u8[^"]*)""""),
            Regex("""file:\s*["']?(https?://[^"'<>\s]+\.m3u8[^"'<>\s]*)"""),
            Regex("""source\s*[=:]\s*["']?(https?://[^"'<>\s]+\.m3u8[^"'<>\s]*)"""),
            Regex("""url:\s*["'](https?://[^"']+\.m3u8[^"']*)"""),
            Regex("""'(https?://[^']+\.mp4[^']*)'"""),
            Regex(""""(https?://[^"]+\.mp4[^"]*)""""),
        )

        for (mirrorUrl in mirrors) {
            val html = fetchHtml(mirrorUrl, ua, "https://tioanime.com/") ?: continue
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

    private fun extractOkru(embedUrl: String, ua: String): String? {
        val html = fetchHtml(embedUrl, ua, "https://tioanime.com/") ?: return null
        val hls = Regex(""""hlsMasterPlaylistUrl":"([^"]+)"""").find(html)
        if (hls != null) return hls.groupValues[1].replace("\\", "")
        val mp4 = Regex(""""mp4":\s*\[.*?"src":"([^"]+)"""", RegexOption.DOT_MATCHES_ALL).find(html)
        if (mp4 != null) return mp4.groupValues[1].replace("\\", "")
        return null
    }

    private fun extractMp4Upload(embedUrl: String, ua: String): String? {
        val html = fetchHtml(embedUrl, ua, "https://tioanime.com/") ?: return null
        val m = Regex("""src:\s*"(https?://[^"]+\.mp4[^"]*)"""").find(html)
        return m?.groupValues?.get(1)
    }

    private fun extractStreamtape(embedUrl: String, ua: String): String? {
        val html = fetchHtml(embedUrl, ua, "https://tioanime.com/") ?: return null
        val link = Regex("""id="ideoooolink"[^>]*>([^<]+)</a>""").find(html)
        if (link != null) return "https:" + link.groupValues[1].trim()
        val base = Regex("""//[^"]*streamtape[^/]+/get_video\?[^"]+""").find(html)
        return base?.let { "https:" + it.value }
    }

    override suspend fun getEpisodes(url: String): List<Episode> = emptyList()

    companion object {
        fun refererFor(url: String): String = when {
            url.contains("yourupload.com") -> "https://www.yourupload.com/"
            url.contains("hqq.tv") || url.contains("netu") -> "https://hqq.tv/"
            url.contains("ok.ru") -> "https://ok.ru/"
            url.contains("streamsb") || url.contains("sbfull") -> "https://streamsb.com/"
            url.contains("fembed") -> "https://www.fembed.com/"
            url.contains("mp4upload") -> "https://www.mp4upload.com/"
            url.contains("streamtape") -> "https://streamtape.com/"
            else -> "https://tioanime.com/"
        }
    }
}
