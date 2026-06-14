package com.kuroanime.extension

import android.util.Base64
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource
import com.kuroanime.extractor.ExtractorRegistry
import com.kuroanime.extractor.VideoExtractorEngine

class LatanimeExtension : JsoupBasedExtension() {
    override val name = "Latanime"
    override val baseUrl = ProviderConfig.getBaseUrl(name) ?: "https://latanime.org"
    override val lang = "es"

    private val videoCache = mutableMapOf<String, List<VideoSource>>()

    override suspend fun getLatest(page: Int): List<Anime> {
        val url = if (page <= 1) baseUrl else "$baseUrl/page/$page/"
        val doc = getDocument(url)
        val seen = mutableSetOf<String>()
        val results = mutableListOf<Anime>()

        doc.select("a[href*='/anime/']").forEach { el ->
            val href = el.attr("href")
            if (!href.contains("/anime/")) return@forEach
            val path = href.substringAfter("://").substringAfter("/")
            if (path.count { it == '/' } > 1) return@forEach
            val title = el.select("[class*=title], [class*=Title], h3, h2, h1, strong, span.title, span.name")
                .firstOrNull()?.text()?.ifBlank { null }
                ?: el.textNodes().firstOrNull { it.text().isNotBlank() }?.text()?.trim()
                ?: return@forEach
            if (title in seen) return@forEach
            seen.add(title)
            val img = el.select("img").attr("data-src").ifBlank { el.select("img").attr("src") }
            results.add(
                Anime(
                    title = title,
                    url = if (href.startsWith("http")) href else "$baseUrl$href",
                    imageUrl = if (img.startsWith("http")) img else "$baseUrl$img".takeIf { img.isNotBlank() },
                    source = name
                )
            )
        }

        return results.distinctBy { it.title.lowercase().trim() }.take(48)
    }

    override suspend fun getLatestEpisodes(): List<LatestEpisode> {
        val doc = getDocument(baseUrl)
        return doc.select("a[href*='/ver/']").mapNotNull { el ->
            val href = el.absUrl("href")
            if (href.isBlank()) return@mapNotNull null
            val titleEl = el.selectFirst("div.info h2")
            val titleText = titleEl?.text()?.trim() ?: return@mapNotNull null
            val epNum = titleText.filter { it.isDigit() }.take(3).toIntOrNull() ?: return@mapNotNull null
            val animeTitle = titleText.substringAfter("-").trim().ifBlank {
                titleText.substringAfter("Episodio").trim()
            }
            val img = el.select("img").attr("data-src")
                .ifBlank { el.select("img").attr("src") }
            val slug = href.substringAfter("/ver/").substringBefore("-episodio-")
            LatestEpisode(
                title = animeTitle,
                episode = "Episodio $epNum",
                image = if (img.startsWith("http")) img else "",
                animeUrl = "$baseUrl/anime/$slug",
                episodeUrl = href,
                source = name,
            )
        }.take(24)
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        val doc = getDocument("$baseUrl/genero/${genre.lowercase()}?p=$page")
        return doc.select("a:has(div.series)").mapNotNull { link ->
            val series = link.selectFirst("div.series") ?: return@mapNotNull null
            val title = series.select("div.seriedetails h3").text().ifBlank { return@mapNotNull null }
            val url = link.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val img = series.select("img").attr("data-src").ifBlank { series.select("img").attr("src") }
            val type = series.select("span.opacity-75").text()
            Anime(
                title = title,
                url = url,
                imageUrl = if (img.startsWith("http")) img else null,
                type = type,
                source = name
            )
        }
    }

    override suspend fun search(query: String): List<Anime> {
        val doc = getDocument("$baseUrl/buscar?q=${query.replace(" ", "+")}")
        return doc.select("a:has(div.series)").mapNotNull { link ->
            val series = link.selectFirst("div.series") ?: return@mapNotNull null
            val title = series.select("div.seriedetails h3").text().ifBlank { return@mapNotNull null }
            val url = link.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val img = series.select("img").attr("data-src").ifBlank { series.select("img").attr("src") }
            Anime(
                title = title,
                url = url,
                imageUrl = if (img.startsWith("http")) img else null,
                source = name
            )
        }.distinctBy { it.title.lowercase().trim() }.take(20)
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        val doc = getDocument(url)
        val title = doc.select("h2").first()?.text() ?: ""
        val synopsis = doc.select("p.my-2.opacity-75").text()
        val img = doc.select("div.serieimgficha img").attr("src")
        val genres = doc.select("a[href*='/genero/'] div.btn").eachText()
        val slug = url.substringAfter("/anime/")

        return Anime(
            title = title,
            url = url,
            imageUrl = if (img.startsWith("http")) img else null,
            synopsis = synopsis,
            genres = genres,
            episodes = parseEpisodesFromDoc(doc, slug),
            source = name
        )
    }

    override suspend fun getEpisodes(url: String): List<Episode> {
        val doc = getDocument(url)
        val slug = url.substringAfter("/anime/")
        return parseEpisodesFromDoc(doc, slug)
    }

    private fun parseEpisodesFromDoc(doc: org.jsoup.nodes.Document, slug: String): List<Episode> {
        return doc.select("a[href*='/ver/$slug-episodio-']").mapNotNull { el ->
            val href = el.attr("href")
            val epNum = href.substringAfter("-episodio-").substringBefore("/").toIntOrNull()
                ?: return@mapNotNull null
            val img = el.select("img").attr("data-src").ifBlank { el.select("img").attr("src") }
            Episode(
                number = epNum,
                title = "Episodio $epNum",
                url = if (href.startsWith("http")) href else "$baseUrl$href",
                imageUrl = if (img.startsWith("http")) img else null
            )
        }.sortedBy { it.number }
    }

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        videoCache[episodeUrl]?.let { return it }

        val doc = getDocument(episodeUrl)
        val embedList = doc.select("a.play-video[data-player]").mapNotNull { el ->
            try {
                val b64 = el.attr("data-player")
                val serverName = el.text().ifBlank { "unknown" }
                val decoded = String(Base64.decode(b64, Base64.DEFAULT))
                val referer = refererFor(decoded)
                val extractor = ExtractorRegistry.findForUrl(decoded)
                if (extractor != null) {
                    val result = VideoExtractorEngine.smartExtractSingle(
                        decoded, referer, null, extractor
                    )
                    if (result.videoUrl != null) {
                        val profile = com.kuroanime.data.network.BrowserProfiles.forHost(result.videoUrl)
                        return@mapNotNull VideoSource(
                            url = result.videoUrl,
                            server = extractor.serverKey,
                            quality = name,
                            headers = com.kuroanime.data.network.BrowserProfiles.basicHeaders(profile, referer),
                            qualities = result.qualities,
                            isM3U8 = result.videoUrl.contains(".m3u8"),
                        )
                    }
                }
                val profile = com.kuroanime.data.network.BrowserProfiles.forHost(decoded)
                VideoSource(
                    url = decoded,
                    server = serverName.lowercase(),
                    quality = name,
                    headers = com.kuroanime.data.network.BrowserProfiles.basicHeaders(profile, refererFor(decoded)),
                )
            } catch (_: Exception) { null }
        }

        val sorted = prioritySort(embedList).take(6)
        if (sorted.isNotEmpty()) videoCache[episodeUrl] = sorted
        return sorted
    }

    override suspend fun getVideoSourceFast(episodeUrl: String): VideoSource? {
        val cached = videoCache[episodeUrl]
        if (cached != null && cached.isNotEmpty()) return cached.first()

        val doc = getDocument(episodeUrl)
        for (el in doc.select("a.play-video[data-player]")) {
            try {
                val b64 = el.attr("data-player")
                val serverName = el.text().ifBlank { "unknown" }
                val decoded = String(Base64.decode(b64, Base64.DEFAULT))
                val extractor = ExtractorRegistry.findForUrl(decoded)
                if (extractor != null) {
                    val result = VideoExtractorEngine.smartExtractSingle(
                        decoded, refererFor(decoded), null, extractor
                    )
                    if (result.videoUrl != null) {
                        val profile = com.kuroanime.data.network.BrowserProfiles.forHost(result.videoUrl)
                        return VideoSource(
                            url = result.videoUrl,
                            server = "fast",
                            quality = name,
                            headers = com.kuroanime.data.network.BrowserProfiles.basicHeaders(profile, "https://latanime.org/"),
                        )
                    }
                }
            } catch (_: Exception) { continue }
        }
        return null
    }

    private fun prioritySort(sources: List<VideoSource>): List<VideoSource> {
        val preferred = listOf("streamtape", "yourupload", "dsvplay", "dood", "netu", "mp4upload", "voe", "mixdrop")
        val top = preferred.mapNotNull { p -> sources.find { it.server.contains(p, true) } }
        val rest = sources.filter { s -> top.none { it.url == s.url } }
        return top + rest
    }

    companion object {
        fun refererFor(url: String): String = when {
            url.contains("mp4upload") -> "https://www.mp4upload.com/"
            url.contains("dood") -> "https://doodstream.com/"
            url.contains("mega") -> "https://mega.nz/"
            url.contains("voe.sx") -> "https://voe.sx/"
            url.contains("mixdrop") -> "https://mixdrop.top/"
            url.contains("hexload") -> "https://hexload.com/"
            url.contains("savefiles") -> "https://savefiles.com/"
            url.contains("byse") -> "https://bysekze.com/"
            else -> "https://latanime.org/"
        }
    }
}
