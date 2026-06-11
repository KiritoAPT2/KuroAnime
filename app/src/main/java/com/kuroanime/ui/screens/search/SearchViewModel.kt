package com.kuroanime.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Anime>>(emptyList())
    val results = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            val exts = ExtensionManager.getAll()
            Log.d("SearchVM", "Searching '$query' in ${exts.size} extensions")
            val allResults = withContext(Dispatchers.IO) {
                exts.map { ext ->
                    async {
                        try {
                            val r = ext.search(query)
                            Log.d("SearchVM", "${ext.name}: ${r.size} results")
                            r
                        } catch (e: Exception) {
                            Log.e("SearchVM", "${ext.name} search failed: ${e.message}")
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
