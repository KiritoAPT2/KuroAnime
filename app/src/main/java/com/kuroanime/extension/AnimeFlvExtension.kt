package com.kuroanime.extension

import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource
import com.kuroanime.data.network.BrowserProfiles
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class AnimeFlvExtension : JsoupBasedExtension() {
    override val name = "AnimeFLV"
    override val baseUrl = ProviderConfig.getBaseUrl(name) ?: "https://www4.animeflv.net"
    override val lang = "es"

    private val persistJson = Json { ignoreUnknownKeys = true }
    private val videoCache = object : LinkedHashMap<String, List<VideoSource>>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<VideoSource>>): Boolean = size > 50
    }
    private val episodeCache = mutableMapOf<String, Pair<Long, List<Episode>>>()
    private val episodePersistTtl = 4 * 60 * 60_000L

    private fun getCachedEpisodes(slug: String): List<Episode>? {
        episodeCache[slug]?.let { (expiry, list) ->
            if (System.currentTimeMillis() < expiry) return list
            episodeCache.remove(slug)
        }
        val persisted = PersistentResultCache.getString("ep_animeflv_$slug")
        if (persisted != null) {
            val parsed = try { persistJson.decodeFromString(ListSerializer(Episode.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) {
                val ttl = if (parsed.any { it.number >= 100 }) 30 * 60_000L else episodePersistTtl
                episodeCache[slug] = System.currentTimeMillis() + ttl to parsed
                return parsed
            }
        }
        return null
    }

    private fun cacheEpisodes(slug: String, episodes: List<Episode>) {
        val ttl = if (episodes.any { it.number >= 100 }) 30 * 60_000L else episodePersistTtl
        episodeCache[slug] = System.currentTimeMillis() + ttl to episodes
        try {
            PersistentResultCache.setString("ep_animeflv_$slug", persistJson.encodeToString(ListSerializer(Episode.serializer()), episodes), ttl)
        } catch (_: Exception) {}
    }

    override suspend fun search(query: String): List<Anime> {
        val doc = getDocument("$baseUrl/browse?q=${query.replace(" ", "+")}")
        return doc.select("article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
            val img = el.select("img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        val doc = getDocument("$baseUrl/browse?genre%5B%5D=${genre.lowercase()}&order=default&page=$page")
        return doc.select("article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
            val img = el.select("img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        val doc = getDocument(url)
        val title = doc.select("h1.Title").text()
        val synopsis = doc.select("div.Description p").text()
        val type = doc.select("span.Type").text()
        val status = doc.select("span.Status").text()
        val score = doc.select("span.Vote").text()
        val img = doc.select("figure img").attr("src")
        val genres = doc.select("nav.NvBn a[href*='?genre']").eachText()
        val slug = url.substringAfter("/anime/")
        val episodes = parseEpisodesFromDoc(doc, slug)
        return Anime(
            title = title, url = url,
            imageUrl = if (img.startsWith("http")) img else "$baseUrl$img",
            synopsis = synopsis, type = type, status = status, score = score,
            genres = genres, episodes = episodes, source = name
        )
    }

    override suspend fun getEpisodes(url: String): List<Episode> {
        val slug = url.substringAfter("/anime/")
        getCachedEpisodes(slug)?.let { return it }
        val doc = getDocument(url)
        val episodes = parseEpisodesFromDoc(doc, slug)
        cacheEpisodes(slug, episodes)
        return episodes
    }

    private fun parseEpisodesFromDoc(doc: org.jsoup.nodes.Document, slug: String): List<Episode> {
        val script = doc.select("script:containsData(var episodes)").first() ?: return emptyList()
        val data = script.data().substringAfter("var episodes = ").substringBefore(";")
        val pairs = json.decodeFromString<List<List<Int>>>(data)
        return pairs.map { (epNum, _) ->
            Episode(number = epNum, title = "Episodio $epNum", url = "$baseUrl/ver/$slug-$epNum")
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheLock = ReentrantLock()

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        cacheLock.withLock {
            videoCache[episodeUrl]?.let { return it }
        }

        val doc = getDocument(episodeUrl)
        val script = doc.select("script:containsData(var videos)").first() ?: return emptyList()
        val text = script.data()
        val prefix = "var videos = "
        val start = text.indexOf(prefix)
        if (start == -1) return emptyList()
        var depth = 0
        var inStr = false
        var esc = false
        var jsonEnd = -1
        for (i in (start + prefix.length) until text.length) {
            val c = text[i]
            if (esc) { esc = false; continue }
            if (c == '\\' && inStr) { esc = true; continue }
            if (c == '"') { inStr = !inStr; continue }
            if (!inStr) {
                if (c == '{') depth++
                else if (c == '}') { depth--; if (depth == 0) { jsonEnd = i + 1; break } }
            }
        }
        if (jsonEnd == -1) return emptyList()
        val jsonStr = text.substring(start + prefix.length, jsonEnd)
        val root = json.decodeFromString<JsonObject>(jsonStr)
        val sources = mutableListOf<VideoSource>()
        for ((lang, element) in root) {
            element.jsonArray.forEach { item ->
                val obj = item.jsonObject
                val url = obj["code"]?.jsonPrimitive?.contentOrNull
                    ?: obj["url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val server = obj["server"]?.jsonPrimitive?.contentOrNull ?: name
                val profile = BrowserProfiles.forHost(url)
                val headers = BrowserProfiles.basicHeaders(profile, refererFor(url, server))
                sources.add(VideoSource(url = url, server = server, quality = lang, headers = headers))
            }
        }
        val result = sources.toList()
        if (result.isNotEmpty()) {
            cacheLock.withLock {
                videoCache[episodeUrl]?.let { return it }
                videoCache[episodeUrl] = result
            }
        }
        return result
    }

    suspend fun getByType(type: String, page: Int = 1): List<Anime> {
        val doc = getDocument("$baseUrl/browse?type%5B%5D=$type&order=default&page=$page")
        return doc.select("article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
            val img = el.select("img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }
    }

    override suspend fun getLatestEpisodes(): List<LatestEpisode> {
        val doc = getDocument("$baseUrl/")
        return doc.select("ul.ListEpisodios li").mapNotNull { item ->
            val episodeUrl = item.selectFirst("a")?.absUrl("href") ?: return@mapNotNull null
            val image = item.selectFirst("img")?.absUrl("src") ?: ""
            val title = item.selectFirst("strong")?.text()
                ?: item.selectFirst("h3")?.text()
                ?: return@mapNotNull null
            val episode = item.selectFirst(".Capi")?.text() ?: ""
            val animeId = episodeUrl.substringAfter("/ver/").substringBeforeLast("-")
            LatestEpisode(
                title = title,
                episode = episode,
                image = image,
                animeUrl = "$baseUrl/anime/$animeId",
                episodeUrl = episodeUrl,
                source = name,
            )
        }
    }

    override suspend fun getLatest(page: Int): List<Anime> {
        val doc = getDocument("$baseUrl/")
        return doc.select("ul.ListAnimes article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
            val img = el.select("figure img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }
    }

    override suspend fun getAiringAnime(): List<Anime> {
        val doc = getDocument(baseUrl)
        val airingItems = doc.select("div.Wdgt.Emision ul.ListSdbr li a").mapNotNull { el ->
            val title = el.ownText().trim().ifBlank { return@mapNotNull null }
            val url = el.absUrl("href").ifBlank { return@mapNotNull null }
            title to url
        }
        val imageMap = doc.select("ul.ListAnimes article.Anime").associate { el ->
            val cardUrl = el.select("a").first()?.absUrl("href") ?: ""
            val img = el.select("figure img").attr("src")
            cardUrl to if (img.startsWith("http")) img else if (img.isNotBlank()) "$baseUrl$img" else ""
        }
        return airingItems.map { (title, url) ->
            Anime(
                title = title,
                url = url,
                imageUrl = imageMap[url].takeIf { it?.isNotBlank() == true },
                source = name,
            )
        }
    }

    override suspend fun getNews(): List<Anime> {
        val doc = getDocument(baseUrl)
        return doc.select("ul.ListAnimes article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.absUrl("href") ?: return@mapNotNull null
            val img = el.select("figure img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }.take(20)
    }

    override suspend fun getVideoSourceFast(episodeUrl: String): VideoSource? {
        cacheLock.withLock {
            val cached = videoCache[episodeUrl]
            if (cached != null && cached.isNotEmpty()) return cached.first()
        }

        val doc = getDocument(episodeUrl)
        val script = doc.select("script:containsData(var videos)").first() ?: return null
        val text = script.data()
        val prefix = "var videos = "
        val start = text.indexOf(prefix)
        if (start == -1) return null
        var depth = 0
        var inStr = false
        var esc = false
        var jsonEnd = -1
        for (i in (start + prefix.length) until text.length) {
            val c = text[i]
            if (esc) { esc = false; continue }
            if (c == '\\' && inStr) { esc = true; continue }
            if (c == '"') { inStr = !inStr; continue }
            if (!inStr) {
                if (c == '{') depth++
                else if (c == '}') { depth--; if (depth == 0) { jsonEnd = i + 1; break } }
            }
        }
        if (jsonEnd == -1) return null
        val jsonStr = text.substring(start + prefix.length, jsonEnd)
        val root = json.decodeFromString<JsonObject>(jsonStr)
        for ((lang, element) in root) {
            val obj = element.jsonArray.firstOrNull()?.jsonObject ?: continue
            val url = obj["code"]?.jsonPrimitive?.contentOrNull
                ?: obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
            val server = obj["server"]?.jsonPrimitive?.contentOrNull ?: name
            val profile = BrowserProfiles.forHost(url)
            val headers = BrowserProfiles.basicHeaders(profile, refererFor(url, server))
            return VideoSource(url = url, server = server, quality = lang, headers = headers)
        }
        return null
    }

    companion object {
        fun refererFor(url: String, server: String): String = when {
            server == "sw" || url.contains("streamwish") -> "https://streamwish.to/"
            server == "stape" || url.contains("streamtape") -> "https://streamtape.com/"
            server == "mega" || url.contains("mega") -> "https://mega.nz/"
            else -> "https://www4.animeflv.net/"
        }
    }
}
