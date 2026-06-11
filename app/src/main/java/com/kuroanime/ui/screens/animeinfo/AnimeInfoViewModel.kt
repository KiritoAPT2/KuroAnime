package com.kuroanime.ui.screens.animeinfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
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

    fun loadAnime(url: String, source: String) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                AnimeAggregator.getAnimeInfo(url, source)
            }
            _anime.value = info
            _episodes.value = info?.episodes ?: emptyList()
        }
    }
}
