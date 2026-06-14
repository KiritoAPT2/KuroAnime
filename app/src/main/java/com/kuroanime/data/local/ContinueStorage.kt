package com.kuroanime.data.local

import com.kuroanime.data.model.ContinueWatching

object ContinueWatchingStorage {
    private const val FILE = "continue_watching"

    suspend fun getAll(): List<ContinueWatching> {
        return LocalStorage.load<ContinueWatching>(FILE)
            .sortedByDescending { it.lastUpdatedAt }
    }

    suspend fun save(entry: ContinueWatching) {
        val list = getAll().toMutableList()
        list.removeAll { it.animeId == entry.animeId && it.episodeNumber == entry.episodeNumber }
        val healed = if (entry.animeImage.isNullOrBlank()) {
            val existing = list.firstOrNull { it.animeId == entry.animeId && !it.animeImage.isNullOrBlank() }
            if (existing != null) entry.copy(animeImage = existing.animeImage) else entry
        } else {
            entry
        }
        list.add(healed.copy(lastUpdatedAt = System.currentTimeMillis()))
        val trimmed = list.takeLast(100)
        LocalStorage.save(FILE, trimmed)
    }

    suspend fun remove(animeId: String, episodeNumber: Int) {
        val list = getAll().filter { it.animeId != animeId || it.episodeNumber != episodeNumber }
        LocalStorage.save(FILE, list)
    }
}
