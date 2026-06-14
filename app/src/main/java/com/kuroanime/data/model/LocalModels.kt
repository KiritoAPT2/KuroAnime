package com.kuroanime.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteAnime(
    val animeId: String,
    val title: String,
    val imageUrl: String,
    val source: String,
    val addedAt: Long
)

@Serializable
data class HistoryEntry(
    val animeId: String,
    val animeTitle: String,
    val animeImage: String?,
    val episodeNumber: Int,
    val episodeTitle: String,
    val source: String,
    val lastWatchedAt: Long
)

@Serializable
data class ContinueWatching(
    val animeId: String,
    val animeTitle: String,
    val animeImage: String?,
    val episodeNumber: Int,
    val episodeTitle: String,
    val episodeUrl: String,
    val source: String,
    val positionMs: Long,
    val durationMs: Long,
    val server: String = "",
    val lastUpdatedAt: Long = 0
)

@Serializable
data class NotificationPreference(
    val animeId: String,
    val animeUrl: String,
    val title: String,
    val imageUrl: String? = null,
    val source: String,
    val lastEpisode: Int,
    val lastCheckedAt: Long = 0L
)
