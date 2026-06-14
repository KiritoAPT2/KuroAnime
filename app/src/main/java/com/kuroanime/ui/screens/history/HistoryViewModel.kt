package com.kuroanime.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.local.HistoryStorage
import com.kuroanime.data.model.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _history.value = HistoryStorage.getAll()
            _isLoading.value = false
        }
    }

    fun remove(animeId: String, episodeNumber: Int) {
        viewModelScope.launch {
            HistoryStorage.remove(animeId, episodeNumber)
            load()
        }
    }

    fun clear() {
        viewModelScope.launch {
            HistoryStorage.clear()
            load()
        }
    }
}
