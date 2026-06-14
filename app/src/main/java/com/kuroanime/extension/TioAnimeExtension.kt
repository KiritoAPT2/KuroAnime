package com.kuroanime.extension

import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource
import com.kuroanime.data.network.BrowserProfiles
import com.kuroanime.extractor.ExtractorRegistry
import com.kuroanime.extractor.VideoExtractorEngine

class TioAnimeExtension : JsoupBasedExtension() {
    override val name = "TioAnime"
    override val baseUrl = ProviderConfig.getBaseUrl(name) ?: "https://tioanime.com"
    override val lang = "es"

    private val videoCache = mutableMapOf<String, List<VideoSource>>()

    override suspend fun search(query: String): List<Anime> {
        val doc = getDocument("$baseUrl/directorio?q=${query.replace(" ", "+")}")
        return doc.select("a[href*='/anime/']").mapNotNull { el ->
            val href = el.attr("href")
            if (!href.startsWith("/anime/") || href.count { it == '/' } > 2) return@mapNotNull null
            val title = el.ownText().ifBlank { el.text() }.ifBlank { return@mapNotNull null }
            val img = el.select("img").attr("src").ifBlank { el.select("img").attr("data-src") }
            Anime(
                title = title,
                url = "$baseUrl$href",
                imageUrl = if (img.startsWith("http")) img else null,
                source = name
            )
        }.distinctBy { it.title.lowercase().trim() }.take(20)
    }

    override suspend fun getLatest(page: Int): List<Anime> {
        val doc = getDocument("$baseUrl/")
        val seen = mutableSetOf<String>()
        val results = mutableListOf<Anime>()

        doc.select("a[href*='/anime/']").forEach { el ->
            val href = el.attr("href")
            if (!href.startsWith("/anime/") || href.count { it == '/' } > 2) return@forEach
            val title = el.ownText().ifBlank { return@forEach }
            val url = "$baseUrl$href"
            if (url in seen) return@forEach
            seen.add(url)
            val img = el.select("img").attr("src").ifBlank { el.select("img").attr("data-src") }
            results.add(
                Anime(
                    title = title,
                    url = url,
                    imageUrl = if (img.startsWith("http")) img else null,
                    source = name
                )
            )
        }

        val ordered = results.reversed()
        val deduped = mutableListOf<Anime>()
        val titles = mutableSetOf<String>()
        for (a in ordered) {
            val key = a.title.lowercase().trim()
            if (key !in titles) { titles.add(key); deduped.add(a) }
        }
        return deduped.take(24)
    }

    override suspend fun getLatestEpisodes(): List<LatestEpisode> {
        val doc = getDocument("$baseUrl/")
        return doc.select("div.episodes a[href*='/ver/']").mapNotNull { el ->
            val href = el.absUrl("href")
            val text = el.text()
            val epNum = text.filter { it.isDigit() }.toIntOrNull() ?: return@mapNotNull null
            val title = el.select("span.title").text().ifBlank {
                text.substringAfter(" ").ifBlank { text }
            }
            val animeSlug = href.substringAfter("/ver/").substringBeforeLast("-")
            val img = el.select("img").attr("src").ifBlank { el.select("img").attr("data-src") }
            LatestEpisode(
                title = title,
                episode = "Episodio $epNum",
                image = if (img.startsWith("http")) img else "",
                animeUrl = "$baseUrl/anime/$animeSlug",
                episodeUrl = href,
                source = name,
            )
        }.take(24)
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        val doc = getDocument(url)
        val title = doc.select("h1.title").text().ifBlank {
            doc.select("h1").text()
        }
        val synopsis = doc.select("p.sinopsis").text().ifBlank {
            doc.select("p").firstOrNull { it.text().length > 50 }?.text() ?: ""
        }
        val img = doc.select("div.thumb img").attr("src").ifBlank {
            doc.select("img[src*='/uploads/']").attr("src")
        }
        val genres = doc.select("a[href*='/directorio?genero=']").eachText()
        val status = doc.select("a.status").text()

        val slug = url.substringAfter("/anime/")
        val episodes = extractEpisodesFromScript(doc, slug)

        return Anime(
            title = title.ifBlank { slug.replace("-", " ") },
            url = url,
            imageUrl = if (img.startsWith("http")) img else "$baseUrl$img".takeIf { img.isNotBlank() },
            synopsis = synopsis,
            genres = genres,
            status = status,
            episodes = episodes.sortedBy { it.number },
            source = name
        )
    }

    override suspend fun getEpisodes(url: String): List<Episode> {
        val slug = url.substringAfter("/anime/")
        val doc = getDocument(url)
        return extractEpisodesFromScript(doc, slug).sortedBy { it.number }
    }

    private fun extractEpisodesFromScript(doc: org.jsoup.nodes.Document, slug: String): List<Episode> {
        val scripts = doc.select("script:containsData(var episodes = [)")
        for (script in scripts) {
            try {
                val data = script.data()
                val start = data.indexOf("var episodes = [")
                if (start == -1) continue
                val end = data.indexOf("];", start)
                if (end == -1) continue
                val arrStr = data.substring(start + "var episodes = ".length, end + 1)
                val nums = arrStr.replace(Regex("[^0-9,]"), "").split(",").mapNotNull { it.toIntOrNull() }
                if (nums.isNotEmpty()) {
                    return nums.map { n ->
                        Episode(
                            number = n,
                            title = "Episodio $n",
                            url = "$baseUrl/ver/$slug-$n"
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        videoCache[episodeUrl]?.let { return it }

        val doc = getDocument(episodeUrl)
        val sources = mutableListOf<VideoSource>()

        downloadLinksFromTable(doc, sources)
        videoScriptSources(doc, sources)

        val sorted = if (sources.isNotEmpty()) {
            prioritySort(sources).take(6)
        } else {
            emptyList()
        }
        if (sorted.isNotEmpty()) videoCache[episodeUrl] = sorted
        return sorted
    }

    private fun downloadLinksFromTable(doc: org.jsoup.nodes.Document, sources: MutableList<VideoSource>) {
        doc.select("table.table-downloads tbody tr").forEach { row ->
            try {
                val serverName = row.select("td").first()?.text()?.ifBlank {
                    row.select("td").first()?.text()
                } ?: return@forEach
                val link = row.select("a.btn-download").first()?.absUrl("href") ?: return@forEach
                sources.add(
                    VideoSource(
                        url = link,
                        server = serverName.lowercase(),
                        quality = name,
                        headers = BrowserProfiles.basicHeaders(BrowserProfiles.forHost(link), refererFor(link)),
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private suspend fun videoScriptSources(doc: org.jsoup.nodes.Document, sources: MutableList<VideoSource>) {
        val scripts = doc.select("script:containsData(var videos = [)")
        for (script in scripts) {
            try {
                val data = script.data()
                val start = data.indexOf("var videos = [")
                if (start == -1) continue
                val end = data.indexOf("];", start)
                if (end == -1) continue
                val arrStr = data.substring(start + "var videos = ".length, end + 1)

                val rawEntries = extractVideosArrayEntries(arrStr)
                for (entry in rawEntries) {
                    try {
                        val parts = entry.split(",").map { it.trim() }
                        if (parts.size < 2) continue
                        val serverName = parts[0].removeSurrounding("\"").removeSurrounding("'")
                        val embedUrl = parts[1].removeSurrounding("\"").removeSurrounding("'")
                            .replace("\\/", "/")
                        if (serverName.isBlank() || embedUrl.isBlank()) continue
                        processEmbedUrl(embedUrl, serverName, sources)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun extractVideosArrayEntries(arrStr: String): List<String> {
        val entries = mutableListOf<String>()
        var depth = 0
        var inStr = false
        var esc = false
        var current = StringBuilder()
        for (c in arrStr) {
            if (esc) { esc = false; current.append(c); continue }
            if (c == '\\' && inStr) { esc = true; current.append(c); continue }
            if (c == '"' || c == '\'') { inStr = !inStr; current.append(c); continue }
            if (!inStr) {
                if (c == '[') { depth++; if (depth == 1) { current = StringBuilder(); continue } }
                if (c == ']') { depth--; if (depth == 0) { entries.add(current.toString()); continue } }
            }
            current.append(c)
        }
        return entries
    }

    private suspend fun processEmbedUrl(embedUrl: String, serverName: String, sources: MutableList<VideoSource>) {
        val extractor = ExtractorRegistry.findForUrl(embedUrl)
        val profile = BrowserProfiles.forHost(embedUrl)
        val headers = BrowserProfiles.basicHeaders(profile, refererFor(embedUrl))
        if (extractor != null) {
            val result = VideoExtractorEngine.smartExtractSingle(embedUrl, refererFor(embedUrl), null, extractor)
            if (result.videoUrl != null) {
                sources.add(
                    VideoSource(
                        url = result.videoUrl,
                        server = extractor.serverKey,
                        quality = name,
                        headers = headers,
                        qualities = result.qualities,
                        isM3U8 = result.videoUrl.contains(".m3u8"),
                    )
                )
            } else {
                sources.add(VideoSource(url = embedUrl, server = extractor.serverKey, quality = name, headers = headers))
            }
        } else {
            sources.add(VideoSource(url = embedUrl, server = serverName.lowercase(), quality = name, headers = headers))
        }
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        val doc = getDocument("$baseUrl/directorio?genero=${genre.lowercase()}&page=$page")
        return doc.select("a[href*='/anime/']").mapNotNull { el ->
            val href = el.attr("href")
            if (!href.startsWith("/anime/") || href.count { it == '/' } > 2) return@mapNotNull null
            val title = el.ownText().ifBlank { el.text() }.ifBlank { return@mapNotNull null }
            val img = el.select("img").attr("src").ifBlank { el.select("img").attr("data-src") }
            Anime(title = title, url = "$baseUrl$href", imageUrl = if (img.startsWith("http")) img else null, source = name)
        }.distinctBy { it.title.lowercase().trim() }.take(20)
    }

    override suspend fun getVideoSourceFast(episodeUrl: String): VideoSource? {
        val cached = videoCache[episodeUrl]
        if (cached != null && cached.isNotEmpty()) return cached.first()
        return getVideoSources(episodeUrl).firstOrNull()
    }

    private fun prioritySort(sources: List<VideoSource>): List<VideoSource> {
        val preferred = listOf("streamtape", "stape", "mp4upload", "yourupload", "netu", "mega", "voe", "mixdrop")
        val top = preferred.mapNotNull { p -> sources.find { it.server.contains(p, true) } }
        val rest = sources.filter { s -> top.none { it.url == s.url } }
        return top + rest
    }

    override suspend fun getAiringAnime(): List<Anime> = emptyList()
    override suspend fun getNews(): List<Anime> = emptyList()

    companion object {
        fun refererFor(url: String): String = when {
            url.contains("yourupload") -> "https://www.yourupload.com/"
            url.contains("hqq.tv") || url.contains("netu") -> "https://hqq.tv/"
            url.contains("ok.ru") -> "https://ok.ru/"
            url.contains("streamsb") || url.contains("sbfull") -> "https://streamsb.com/"
            url.contains("mp4upload") -> "https://www.mp4upload.com/"
            url.contains("streamtape") || url.contains("stape") -> "https://streamtape.com/"
            url.contains("dood") -> "https://doodstream.com/"
            url.contains("voe.sx") -> "https://voe.sx/"
            url.contains("mega") -> "https://mega.nz/"
            else -> "https://tioanime.com/"
        }
    }
}
