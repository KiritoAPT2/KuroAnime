package com.kuroanime.extension

import android.util.Base64
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource

class LatanimeExtension : JsoupBasedExtension() {
    override val name = "Latanime"
    override val baseUrl = "https://latanime.org"
    override val lang = "es"

    override suspend fun getLatest(page: Int): List<Anime> {
        val doc = getDocument(baseUrl)
        val seen = mutableSetOf<String>()
        val results = mutableListOf<Anime>()

        doc.select("a[href*='/ver/']").forEach { el ->
            val href = el.attr("href")
            val slug = href.substringAfter("/ver/").substringBefore("-episodio-")
            if (slug.isBlank() || slug in seen) return@forEach
            seen.add(slug)

            val episodeTitle = el.select("h2").text().ifBlank { return@forEach }
            val animeTitle = episodeTitle.substringAfter(" - ").ifBlank { episodeTitle }
            val img = el.select("img").attr("data-src").ifBlank { el.select("img").attr("src") }
            val animeUrl = "$baseUrl/anime/$slug"

            results.add(
                Anime(
                    title = animeTitle,
                    url = animeUrl,
                    imageUrl = if (img.startsWith("http")) img else null,
                    source = name
                )
            )
        }

        if (results.isEmpty()) {
            doc.select("div.carousel-item a[href*='/anime/']").forEach { el ->
                val href = el.attr("href")
                val title = el.select("span.span-slider").text().ifBlank { return@forEach }
                if (href in seen) return@forEach
                seen.add(href)
                val img = el.select("img.preview-image").attr("data-src")
                    .ifBlank { el.select("img").attr("src") }
                results.add(
                    Anime(
                        title = title,
                        url = href,
                        imageUrl = if (img.startsWith("http")) img else null,
                        source = name
                    )
                )
            }
        }

        return results.take(24)
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
        }
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
        val doc = getDocument(episodeUrl)
        val sources = mutableListOf<VideoSource>()

        doc.select("a.play-video[data-player]").forEach { el ->
            val b64 = el.attr("data-player")
            val server = el.text().ifBlank { "unknown" }
            try {
                val decoded = Base64.decode(b64, Base64.DEFAULT)
                val url = String(decoded, Charsets.UTF_8)
                sources.add(VideoSource(url = url, server = server))
            } catch (_: Exception) { }
        }

        return sources
    }
}
