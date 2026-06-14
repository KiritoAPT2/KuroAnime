package com.kuroanime.extension

import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource

interface AnimeExtension {
    val name: String
    val baseUrl: String
    val lang: String

    suspend fun search(query: String): List<Anime>
    suspend fun getAnimeInfo(url: String): Anime
    suspend fun getEpisodes(url: String): List<Episode>
    suspend fun getVideoSources(episodeUrl: String): List<VideoSource>
    suspend fun getLatest(page: Int): List<Anime>
    suspend fun getByGenre(genre: String, page: Int): List<Anime>
    suspend fun getLatestEpisodes(): List<LatestEpisode>
    suspend fun getAiringAnime(): List<Anime>
    suspend fun getNews(): List<Anime>

    suspend fun getVideoSourceFast(episodeUrl: String): VideoSource? = null
}
