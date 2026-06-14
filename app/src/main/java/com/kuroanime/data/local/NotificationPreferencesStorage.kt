package com.kuroanime.data.local

import com.kuroanime.data.model.NotificationPreference

object NotificationPreferencesStorage {
    private const val FILE = "notification_prefs"

    suspend fun getAll(): List<NotificationPreference> = LocalStorage.load(FILE)

    suspend fun add(pref: NotificationPreference) {
        val list = getAll().toMutableList()
        list.removeAll { it.animeId == pref.animeId }
        list.add(pref)
        LocalStorage.save(FILE, list.takeLast(50))
    }

    suspend fun remove(animeId: String) {
        val list = getAll().toMutableList()
        list.removeAll { it.animeId == animeId }
        LocalStorage.save(FILE, list)
    }

    suspend fun isNotifying(animeId: String): Boolean {
        return getAll().any { it.animeId == animeId }
    }

    suspend fun get(animeId: String): NotificationPreference? {
        return getAll().find { it.animeId == animeId }
    }

    suspend fun updateLastEpisode(animeId: String, episode: Int) {
        val pref = get(animeId) ?: return
        add(pref.copy(lastEpisode = episode, lastCheckedAt = System.currentTimeMillis()))
    }
}
