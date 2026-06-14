package com.kuroanime.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.local.FavoritesStorage
import com.kuroanime.data.model.FavoriteAnime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    private val _favorites = MutableStateFlow<List<FavoriteAnime>>(emptyList())
    val favorites = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _favorites.value = FavoritesStorage.getAll()
            _isLoading.value = false
        }
    }

    fun remove(animeId: String) {
        viewModelScope.launch {
            FavoritesStorage.remove(animeId)
            load()
        }
    }
}
