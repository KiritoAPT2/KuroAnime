package com.kuroanime.data.model

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

data class Episode(
    val number: Int,
    val title: String,
    val url: String,
    val imageUrl: String? = null
)

data class VideoSource(
    val url: String,
    val quality: String = "default",
    val server: String = "default",
    val headers: Map<String, String>? = null
)

data class Extension(
    val name: String,
    val icon: String? = null,
    val language: String = "es",
    val baseUrl: String = ""
)
