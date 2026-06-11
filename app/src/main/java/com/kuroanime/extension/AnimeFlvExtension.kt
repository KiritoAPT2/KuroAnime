package com.kuroanime.extension

import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class AnimeFlvExtension : JsoupBasedExtension() {
    override val name = "AnimeFLV"
    override val baseUrl = "https://www4.animeflv.net"
    override val lang = "es"

    override suspend fun search(query: String): List<Anime> {
        val doc = getDocument("$baseUrl/browse?q=${query.replace(" ", "+")}")
        return doc.select("article.Anime").mapNotNull { el ->
            val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
            val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
            val img = el.select("img").attr("src")
            Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
        }
    }

    private val genreIds = mapOf(
        "accion" to "1", "aventura" to "2", "romance" to "4", "isekai" to "5"
    )

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        val id = genreIds[genre.lowercase()] ?: return emptyList()
        val doc = getDocument("$baseUrl/browse?genre=$id&page=$page")
        if (doc.select("article.Anime").isNotEmpty()) {
            return doc.select("article.Anime").mapNotNull { el ->
                val title = el.select("h3.Title").text().ifBlank { return@mapNotNull null }
                val url = el.select("a").first()?.attr("href")?.let { "$baseUrl$it" } ?: return@mapNotNull null
                val img = el.select("img").attr("src")
                Anime(title = title, url = url, imageUrl = if (img.startsWith("http")) img else "$baseUrl$img", source = name)
            }
        }
        return search(genre)
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
        val doc = getDocument(url)
        val slug = url.substringAfter("/anime/")
        return parseEpisodesFromDoc(doc, slug)
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

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
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
                sources.add(VideoSource(url = url, server = server, quality = lang))
            }
        }
        return sources
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
}
