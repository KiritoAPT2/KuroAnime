package com.kuroanime.data.local

import com.kuroanime.data.model.HistoryEntry

object HistoryStorage {
    private const val FILE = "history"

    suspend fun getAll(): List<HistoryEntry> {
        return LocalStorage.load<HistoryEntry>(FILE).sortedByDescending { it.lastWatchedAt }
    }

    suspend fun add(entry: HistoryEntry) {
        val list = getAll().toMutableList()
        list.removeAll { it.animeId == entry.animeId && it.episodeNumber == entry.episodeNumber }
        list.add(entry)
        LocalStorage.save(FILE, list.takeLast(200))
    }

    suspend fun remove(animeId: String, episodeNumber: Int) {
        val list = getAll().filter { it.animeId != animeId || it.episodeNumber != episodeNumber }
        LocalStorage.save(FILE, list)
    }

    suspend fun clear() {
        LocalStorage.save<HistoryEntry>(FILE, emptyList())
    }
}
