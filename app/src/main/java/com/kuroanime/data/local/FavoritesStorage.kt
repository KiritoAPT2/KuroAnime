package com.kuroanime.data.local

import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.FavoriteAnime

object FavoritesStorage {
    private const val FILE = "favorites"

    suspend fun getAll(): List<FavoriteAnime> = LocalStorage.load(FILE)

    suspend fun isFavorite(animeId: String): Boolean {
        return getAll().any { it.animeId == animeId }
    }

    suspend fun add(anime: Anime) {
        val list = getAll().toMutableList()
        if (list.none { it.animeId == anime.url }) {
            list.add(
                FavoriteAnime(
                    animeId = anime.url,
                    title = anime.title,
                    imageUrl = anime.imageUrl ?: "",
                    source = anime.source,
                    addedAt = System.currentTimeMillis()
                )
            )
            LocalStorage.save(FILE, list)
        }
    }

    suspend fun remove(animeId: String) {
        val list = getAll().filter { it.animeId != animeId }
        LocalStorage.save(FILE, list)
    }

    suspend fun toggle(anime: Anime): Boolean {
        val exists = isFavorite(anime.url)
        if (exists) remove(anime.url) else add(anime)
        return !exists
    }
}
