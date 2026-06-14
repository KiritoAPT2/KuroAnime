package com.kuroanime.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.ResultCache
import com.kuroanime.data.local.ContinueWatchingStorage
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.extension.AnimeAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ExploreViewModel : ViewModel() {
    private val _continueWatching = MutableStateFlow<List<ContinueWatching>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    private val _trending = MutableStateFlow<List<Anime>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _latestEpisodes = MutableStateFlow<List<LatestEpisode>>(emptyList())
    val latestEpisodes = _latestEpisodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadContinueWatching()
        loadTrending()
        loadLatestEpisodes()
    }

    fun refresh() {
        loadContinueWatching()
        loadTrending()
        loadLatestEpisodes()
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            _continueWatching.value = ContinueWatchingStorage.getAll()
        }
    }

    private fun loadTrending() {
        val cached = ResultCache.get<List<Anime>>("explore_trending")
        if (cached != null) {
            _trending.value = cached
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                try {
                    withTimeout(5000) { AnimeAggregator.getLatest().take(20) }
                } catch (_: Exception) { emptyList() }
            }
            ResultCache.set("explore_trending", result, 900_000L)
            _trending.value = result
            _isLoading.value = false
        }
    }

    private fun loadLatestEpisodes() {
        val cached = ResultCache.get<List<LatestEpisode>>("explore_latest_episodes")
        if (cached != null) {
            _latestEpisodes.value = cached
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    withTimeout(5000) { AnimeAggregator.getLatestEpisodes().take(20) }
                } catch (_: Exception) { emptyList() }
            }
            ResultCache.set("explore_latest_episodes", result, 900_000L)
            _latestEpisodes.value = result
        }
    }

    companion object {
        val genreChips = listOf(
            "Acción", "Artes Marciales", "Aventuras", "Carreras",
            "Ciencia Ficción", "Comedia", "Demencia", "Demonios",
            "Deportes", "Drama", "Ecchi", "Escolares",
            "Espacial", "Fantasía", "Harem", "Historico",
            "Infantil", "Josei", "Juegos", "Magia",
            "Mecha", "Militar", "Misterio", "Música",
            "Parodia", "Policía", "Psicológico", "Recuentos de la vida",
            "Romance", "Samurai", "Seinen", "Shoujo",
            "Shounen", "Sobrenatural", "Superpoderes", "Suspenso",
            "Terror", "Vampiros", "Yaoi", "Yuri",
        )
    }
}
