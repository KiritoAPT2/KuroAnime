package com.kuroanime.ui.screens.animeinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.ResultCache
import com.kuroanime.data.anilist.AnimeEnricher
import com.kuroanime.data.anilist.CachedRelation
import com.kuroanime.data.local.FavoritesStorage
import com.kuroanime.data.local.NotificationPreferencesStorage
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.NotificationPreference
import com.kuroanime.extension.AnimeAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimeInfoViewModel : ViewModel() {
    private val _anime = MutableStateFlow<Anime?>(null)
    val anime = _anime.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    private val _isNotifying = MutableStateFlow(false)
    val isNotifying = _isNotifying.asStateFlow()

    private val _relations = MutableStateFlow<List<CachedRelation>>(emptyList())
    val relations = _relations.asStateFlow()

    private val _enrichedGenres = MutableStateFlow<List<String>>(emptyList())
    val enrichedGenres = _enrichedGenres.asStateFlow()

    private var _source: String = ""

    fun loadAnime(url: String, source: String) {
        _source = source
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            val info = withContext(Dispatchers.IO) {
                AnimeAggregator.getAnimeInfo(url, source)
            }
            _anime.value = info
            _episodes.value = info?.episodes ?: emptyList()
            _isLoadingEpisodes.value = false
            if (info != null) {
                _isFavorite.value = FavoritesStorage.isFavorite(info.url)
                _isNotifying.value = NotificationPreferencesStorage.isNotifying(info.url)
                enrichWithAniList(info)
            }
        }
    }

    private suspend fun enrichWithAniList(anime: Anime) {
        val enriched = AnimeEnricher.enrich(anime)
        _enrichedGenres.value = enriched.genres
        _relations.value = enriched.relations.filter {
            it.relationType in listOf("SEQUEL", "PREQUEL", "SIDE_STORY", "SUMMARY", "ALTERNATIVE")
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val anime = _anime.value ?: return@launch
            val newState = FavoritesStorage.toggle(anime)
            _isFavorite.value = newState
        }
    }

    fun toggleNotification() {
        viewModelScope.launch {
            val anime = _anime.value ?: return@launch
            val currentlyNotifying = NotificationPreferencesStorage.isNotifying(anime.url)
            if (currentlyNotifying) {
                NotificationPreferencesStorage.remove(anime.url)
                _isNotifying.value = false
            } else {
                val maxEp = _episodes.value.maxOfOrNull { it.number } ?: 0
                NotificationPreferencesStorage.add(
                    NotificationPreference(
                        animeId = anime.url,
                        animeUrl = anime.url,
                        title = anime.title,
                        imageUrl = anime.imageUrl,
                        source = _source,
                        lastEpisode = maxEp
                    )
                )
                _isNotifying.value = true
            }
        }
    }

    private val _isRefreshingEpisodes = MutableStateFlow(false)
    val isRefreshingEpisodes = _isRefreshingEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(true)
    val isLoadingEpisodes = _isLoadingEpisodes.asStateFlow()

    fun reloadEpisodes() {
        val anime = _anime.value ?: return
        if (_source.isBlank()) return
        viewModelScope.launch {
            _isRefreshingEpisodes.value = true
            _isLoadingEpisodes.value = true
            val cacheKey = "episodes_${_source}_${anime.url.hashCode()}"
            ResultCache.remove(cacheKey)
            val episodes = withContext(Dispatchers.IO) {
                AnimeAggregator.getEpisodes(anime.url, _source)
            }
            _episodes.value = episodes
            _isLoadingEpisodes.value = false
            _isRefreshingEpisodes.value = false
        }
    }

    fun searchAnimeByTitle(title: String, onResult: (Anime?) -> Unit) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) { AnimeAggregator.search(title) }
            val match = results.firstOrNull()
            onResult(match)
        }
    }
}
