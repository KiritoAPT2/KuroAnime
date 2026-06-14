package com.kuroanime.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class SearchViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Anime>>(emptyList())
    val results = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedSource = MutableStateFlow("Todos")
    val selectedSource = _selectedSource.asStateFlow()

    private var searchJob: Job? = null

    val chips = listOf("Todos", "Sub 1", "Sub 2", "Latan")

    private val chipToProvider = mapOf(
        "Sub 1" to "AnimeFLV",
        "Sub 2" to "TioAnime",
        "Latan" to "Latanime",
    )

    fun setSource(source: String) {
        _selectedSource.value = source
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            val filter = _selectedSource.value
            val actualFilter = chipToProvider[filter] ?: filter
            val allExts = ExtensionManager.getAll()
            val exts = if (filter == "Todos") allExts else allExts.filter { it.name == actualFilter }
            val allResults = withContext(Dispatchers.IO) {
                exts.map { ext ->
                    async {
                        try {
                            val timeoutMs = if (ext.name == "TioAnime") 5000L else 10000L
                            withTimeout(timeoutMs) { ext.search(query) }
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }.flatMap { it.await() }
            }
            _results.value = allResults
            _isLoading.value = false
        }
    }
}
