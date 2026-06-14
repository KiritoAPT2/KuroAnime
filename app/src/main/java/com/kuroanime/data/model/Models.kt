package com.kuroanime.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LatestEpisode(
    val title: String,
    val episode: String,
    val image: String = "",
    val animeUrl: String = "",
    val episodeUrl: String = "",
    val source: String = "",
)

@Serializable
data class Anime(
    val title: String,
    val url: String,
    val imageUrl: String? = null,
    val synopsis: String? = null,
    val type: String? = null,
    val status: String? = null,
    val score: String? = null,
    val genres: List<String> = emptyList(),
    val audio: String? = null,
    val episodes: List<Episode> = emptyList(),
    val source: String = ""
)

@Serializable
data class Episode(
    val number: Int,
    val title: String,
    val url: String,
    val imageUrl: String? = null
)

@Serializable
data class QualityOption(
    val label: String,
    val url: String,
    val bandwidth: Long = 0,
)

@Serializable
data class VideoSource(
    val url: String,
    val quality: String = "default",
    val server: String = "default",
    val headers: Map<String, String>? = null,
    val qualities: List<QualityOption> = emptyList(),
    val isM3U8: Boolean = false,
)

@Serializable
data class Extension(
    val name: String,
    val icon: String? = null,
    val language: String = "es",
    val baseUrl: String = ""
)
