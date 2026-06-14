package com.kuroanime.data.anilist

import android.util.Log
import com.kuroanime.data.model.Anime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnimeEnricher {
    private const val TAG = "AnimeEnricher"

    suspend fun enrich(anime: Anime): CachedAnimeData {
        val key = normalize(anime.title)
        val cached = AniListCache.get(key)
        if (cached != null) {
            Log.d(TAG, "enrich cache HIT key=$key genres=${cached.genres}")
            return cached
        }
        Log.d(TAG, "enrich cache MISS key=$key title=\"${anime.title}\"")

        val media = withContext(Dispatchers.IO) {
            runCatching { AniListClient.findAnimeByTitle(anime.title) }.getOrNull()
        }
        if (media == null) {
            Log.w(TAG, "enrich AniList returned null for \"${anime.title}\" — NOT caching")
            return CachedAnimeData(title = anime.title, genres = anime.genres)
        }

        val data = CachedAnimeData(
            title = anime.title,
            genres = media.genres ?: anime.genres,
            tags = emptyList(),
            score = media.averageScore,
            synopsis = media.description,
            episodes = media.episodes,
            year = null,
            season = null,
            format = media.format,
            status = media.status,
            relations = media.relations?.edges?.mapNotNull { edge ->
                val title = edge.node.title?.english ?: edge.node.title?.romaji ?: return@mapNotNull null
                CachedRelation(
                    title = title,
                    anilistId = edge.node.id,
                    relationType = edge.relationType,
                    imageUrl = edge.node.coverImage?.large
                )
            } ?: emptyList()
        )

        Log.d(TAG, "enrich caching key=$key genres=${data.genres} relations=${data.relations.size}")
        AniListCache.put(key, data)
        return data
    }

    fun enrichSync(anime: Anime): CachedAnimeData {
        val key = normalize(anime.title)
        return AniListCache.getCached(key) ?: CachedAnimeData(title = anime.title, genres = anime.genres)
    }

    fun normalize(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun enrichAll(animes: List<Anime>): Map<String, CachedAnimeData> {
        val result = mutableMapOf<String, CachedAnimeData>()
        for (anime in animes) {
            result[anime.title] = enrich(anime)
        }
        return result
    }
}
