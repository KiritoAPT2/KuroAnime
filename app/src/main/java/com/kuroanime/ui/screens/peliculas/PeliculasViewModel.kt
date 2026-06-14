package com.kuroanime.ui.screens.peliculas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.ExtensionManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val CACHE_KEY = "peliculas"
private const val CACHE_TTL = 7 * 24 * 60 * 60 * 1000L // 7 días

private val peliculasJson = Json { ignoreUnknownKeys = true }

class PeliculasViewModel : ViewModel() {
    private val _peliculas = MutableStateFlow<List<Anime>>(emptyList())
    val peliculas = _peliculas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        val cached = ResultCache.get<List<Anime>>(CACHE_KEY)
        if (cached != null) {
            _peliculas.value = cached
            _isLoading.value = false
        } else {
            val persisted = PersistentResultCache.getString(CACHE_KEY)
            if (persisted != null) {
                try {
                    val parsed = peliculasJson.decodeFromString(ListSerializer(Anime.serializer()), persisted)
                    ResultCache.set(CACHE_KEY, parsed, CACHE_TTL)
                    _peliculas.value = parsed
                    _isLoading.value = false
                } catch (_: Exception) {
                    load()
                }
            } else {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val flv = ExtensionManager.getByName("AnimeFLV")
            val results = withContext(Dispatchers.IO) {
                try {
                    withTimeout(10000) {
                        val movies = (flv as? com.kuroanime.extension.AnimeFlvExtension)?.getByType("movie", 1)?.take(40) ?: emptyList()
                        if (movies.isNotEmpty()) movies
                        else (flv as? com.kuroanime.extension.AnimeFlvExtension)?.getByType("anime", 1)?.take(40) ?: emptyList()
                    }
                } catch (_: Exception) { emptyList() }
            }
            if (results.isNotEmpty()) {
                ResultCache.set(CACHE_KEY, results, CACHE_TTL)
                try { PersistentResultCache.setString(CACHE_KEY, peliculasJson.encodeToString(ListSerializer(Anime.serializer()), results), CACHE_TTL) } catch (_: Exception) {}
            }
            _peliculas.value = results
            _isLoading.value = false
        }
    }
}
