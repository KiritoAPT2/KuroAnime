package com.kuroanime.extension

import com.kuroanime.data.HttpClient
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

abstract class JsoupBasedExtension : AnimeExtension {

    protected suspend fun getDocument(url: String): org.jsoup.nodes.Document = withContext(Dispatchers.IO) {
        val response = HttpClient.client.newCall(
            Request.Builder().url(url).build()
        ).execute()
        Jsoup.parse(response.body?.string() ?: "", url)
    }

    override suspend fun search(query: String): List<Anime> = emptyList()
    override suspend fun getAnimeInfo(url: String): Anime = Anime(title = "", url = url)
    override suspend fun getEpisodes(url: String): List<Episode> = emptyList()
    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> = emptyList()
    override suspend fun getLatest(page: Int): List<Anime> = emptyList()
    override suspend fun getByGenre(genre: String, page: Int): List<Anime> = emptyList()
    override suspend fun getLatestEpisodes(): List<LatestEpisode> = emptyList()
    override suspend fun getAiringAnime(): List<Anime> = emptyList()
    override suspend fun getNews(): List<Anime> = emptyList()
}
