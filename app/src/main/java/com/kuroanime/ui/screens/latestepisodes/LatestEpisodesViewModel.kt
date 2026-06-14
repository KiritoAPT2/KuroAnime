package com.kuroanime.ui.screens.latestepisodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.extension.AnimeAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LatestEpisodesViewModel : ViewModel() {
    private val _episodes = MutableStateFlow<List<LatestEpisode>>(emptyList())
    val episodes: StateFlow<List<LatestEpisode>> = _episodes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _episodes.value = AnimeAggregator.getLatestEpisodes()
            _isLoading.value = false
        }
    }
}
