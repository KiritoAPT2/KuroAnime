package com.kuroanime.data.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
)

@Serializable
data class AniListRelationNode(
    val id: Long,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val type: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val averageScore: Int? = null
)

@Serializable
data class AniListRelationEdge(
    val relationType: String,
    val node: AniListRelationNode
)

@Serializable
data class AniListRelations(
    val edges: List<AniListRelationEdge> = emptyList()
)

@Serializable
data class AniListMedia(
    val id: Long? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val format: String? = null,
    val relations: AniListRelations? = null
)

@Serializable
data class AniListMediaResponse(
    val Media: AniListMedia
)

@Serializable
data class AniListPageInfo(
    val total: Int? = null,
    val currentPage: Int? = null,
    val lastPage: Int? = null,
    val hasNextPage: Boolean? = null
)

@Serializable
data class AniListNextAiringEpisode(
    val airingAt: Long? = null,
    val episode: Int? = null,
    val timeUntilAiring: Long? = null
)

@Serializable
data class AniListPageMedia(
    val id: Long,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val nextAiringEpisode: AniListNextAiringEpisode? = null
)

@Serializable
data class AniListPage(
    val pageInfo: AniListPageInfo? = null,
    val media: List<AniListPageMedia> = emptyList()
)

@Serializable
data class AniListSearchResponse(
    val Page: AniListPage? = null
)

@Serializable
data class AniListGraphQLResponse<T>(
    val data: T
)

data class AniListError(
    val message: String? = null,
    val status: Int? = null
)
