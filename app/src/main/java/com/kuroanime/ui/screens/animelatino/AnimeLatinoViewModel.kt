package com.kuroanime.ui.screens.animelatino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val CACHE_KEY = "anime_latino_list"
private const val CACHE_TTL = 24 * 60 * 60 * 1000L

private val latinoJson = Json { ignoreUnknownKeys = true }

class AnimeLatinoViewModel : ViewModel() {
    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var loaded = false

    init {
        val cached = ResultCache.get<List<Anime>>(CACHE_KEY)
        if (cached != null) {
            _animeList.value = cached
            loaded = true
        } else {
            val persisted = PersistentResultCache.getString(CACHE_KEY)
            if (persisted != null) {
                try {
                    val parsed = latinoJson.decodeFromString(ListSerializer(Anime.serializer()), persisted)
                    ResultCache.set(CACHE_KEY, parsed, CACHE_TTL)
                    _animeList.value = parsed
                    loaded = true
                } catch (_: Exception) {}
            }
        }
    }

    fun load() {
        if (loaded) {
            val stale = ResultCache.get<List<Anime>>(CACHE_KEY)
            if (stale != null) _animeList.value = stale
        }
        loaded = true

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = withContext(Dispatchers.IO) {
                try {
                    val ext = ExtensionManager.getByName("Latanime")
                    if (ext != null) {
                        val allResults = mutableListOf<Anime>()
                        val seen = mutableSetOf<String>()
                        for (page in 1..3) {
                            val pageResults = withTimeout(8000L) {
                                ext.getLatest(page)
                            }
                            val newOnes = pageResults.filter { a ->
                                val key = a.title.lowercase().trim()
                                (key !in seen).also { if (it) seen.add(key) }
                            }
                            allResults.addAll(newOnes)
                            if (pageResults.size < 24) break
                        }
                        allResults
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) { emptyList() }
            }
            if (result.isNotEmpty()) {
                ResultCache.set(CACHE_KEY, result, CACHE_TTL)
                try { PersistentResultCache.setString(CACHE_KEY, latinoJson.encodeToString(ListSerializer(Anime.serializer()), result), CACHE_TTL) } catch (_: Exception) {}
                _animeList.value = result
                _error.value = null
            } else if (_animeList.value.isEmpty()) {
                _error.value = "No se pudieron cargar animes latinos"
            }
            _isLoading.value = false
        }
    }
}
