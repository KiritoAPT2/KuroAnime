package com.kuroanime.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.ResultCache
import com.kuroanime.data.local.ContinueWatchingStorage
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.extension.AnimeAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GenreSection(
    val genre: String,
    val title: String,
    val items: List<Anime> = emptyList(),
    val isLoading: Boolean = false,
)

class HomeViewModel : ViewModel() {
    private val _continueWatching = MutableStateFlow<List<ContinueWatching>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    private val _latestEpisodes = MutableStateFlow<List<LatestEpisode>>(emptyList())
    val latestEpisodes = _latestEpisodes.asStateFlow()

    private val _popularAnime = MutableStateFlow<List<Anime>>(emptyList())
    val popularAnime = _popularAnime.asStateFlow()

    private val _recommendedAnime = MutableStateFlow<List<Anime>>(emptyList())
    val recommendedAnime = _recommendedAnime.asStateFlow()

    private val _generosPopulares = MutableStateFlow<List<GenreSection>>(emptyList())
    val generosPopulares = _generosPopulares.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _randomAnimeEvent = Channel<Anime>(Channel.CONFLATED)
    val randomAnimeEvent = _randomAnimeEvent.receiveAsFlow()

    private val generosConfig = listOf(
        "accion" to "Acción",
        "aventura" to "Aventura",
        "fantasia" to "Fantasía",
    )

    init {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _isLoading.value = true
            loadContinueWatchingInternal()
            loadHomeData()
            _isLoading.value = false
        }
    }

    private suspend fun loadHomeData() {
        val homeData = withContext(Dispatchers.IO) {
            AnimeAggregator.getHomeData()
        }
        _latestEpisodes.value = homeData.latestEpisodes
        _popularAnime.value = homeData.populares
        _recommendedAnime.value = homeData.recomendaciones
        _generosPopulares.value = generosConfig.map { (slug, title) ->
            GenreSection(genre = slug, title = title)
        }
    }

    fun loadGenero(slug: String) {
        viewModelScope.launch {
            val current = _generosPopulares.value.toMutableList()
            val idx = current.indexOfFirst { it.genre == slug }
            if (idx == -1 || current[idx].items.isNotEmpty() || current[idx].isLoading) return@launch
            current[idx] = current[idx].copy(isLoading = true)
            _generosPopulares.value = current

            val items = withContext(Dispatchers.IO) {
                AnimeAggregator.getByGenreFLV(slug)
            }

            val updated = _generosPopulares.value.toMutableList()
            val updateIdx = updated.indexOfFirst { it.genre == slug }
            if (updateIdx != -1) {
                updated[updateIdx] = updated[updateIdx].copy(items = items, isLoading = false)
                _generosPopulares.value = updated
            }
        }
    }

    fun loadContinueWatching() {
        viewModelScope.launch { loadContinueWatchingInternal() }
    }

    private suspend fun loadContinueWatchingInternal() {
        _continueWatching.value = ContinueWatchingStorage.getAll()
    }

    fun removeContinueWatching(animeId: String, episodeNumber: Int) {
        viewModelScope.launch {
            ContinueWatchingStorage.remove(animeId, episodeNumber)
            loadContinueWatchingInternal()
        }
    }

    fun loadRandomAnime() {
        viewModelScope.launch {
            val anime = withContext(Dispatchers.IO) {
                AnimeAggregator.getRandomAnime()
            }
            if (anime != null) {
                _randomAnimeEvent.send(anime)
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            ResultCache.clear()
            PersistentResultCache.clear()
            loadHome()
            _isRefreshing.value = false
        }
    }
}
