package com.kuroanime.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.AnimeAggregator
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class HomeViewModel : ViewModel() {
    private val _selectedCategory = MutableStateFlow("inicio")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCategory("inicio")
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadCategory(category)
        }
    }

    private fun loadCategory(category: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val categoryStart = System.currentTimeMillis()
            _isLoading.value = true
            _animeList.value = emptyList()
            Log.d("HomeVM", "Loading category: $category")

            when (category) {
                "inicio" -> {
                    var firstBatch = true
                    AnimeAggregator.getLatestFlow().collect { list ->
                        _animeList.value = list
                        if (list.isNotEmpty() && firstBatch) {
                            firstBatch = false
                            _isLoading.value = false
                            Log.d("PERF", "First anime batch rendered: ${System.currentTimeMillis() - categoryStart}ms")
                        }
                    }
                }
                "emision" -> {
                    val ongoing = AnimeAggregator.getLatest().filter {
                        it.status?.lowercase()?.contains("emisi") == true
                    }.ifEmpty { AnimeAggregator.getLatest().take(30) }
                    _animeList.value = ongoing
                    _isLoading.value = false
                }
                "latinos" -> {
                    val latanime = ExtensionManager.getByName("Latanime")
                    val results = try {
                        withContext(Dispatchers.IO) {
                            withTimeout(8000) {
                                val r = latanime?.getByGenre("latino", 1)?.take(30) ?: emptyList()
                                Log.d("HomeVM", "Latanime.getByGenre(latino): ${r.size} results")
                                r
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeVM", "Latanime.getByGenre(latino) failed: ${e::class.simpleName} - ${e.message}")
                        e.stackTraceToString().lines().take(3).forEach { Log.e("HomeVM", "  $it") }
                        emptyList()
                    }
                    _animeList.value = results
                    _isLoading.value = false
                }
                else -> loadGenreProgressively(category)
            }
        }
    }

    private suspend fun loadGenreProgressively(genre: String) {
        val exts = ExtensionManager.getAll()
        val resultsMap = mutableMapOf<Int, List<Anime>>()
        var completed = 0
        coroutineScope {
            exts.forEachIndexed { index, ext ->
                launch(Dispatchers.IO) {
                    try {
                        val timeout = if (ext.name == "TioAnime") 3000L else 5000L
                        val r = withTimeout(timeout) { ext.getByGenre(genre, 1) }
                        Log.d("HomeVM", "${ext.name}.getByGenre($genre): ${r.size} results")
                        resultsMap[index] = r
                    } catch (e: Exception) {
                        Log.e("HomeVM", "${ext.name}.getByGenre($genre) failed: ${e.message}")
                        resultsMap[index] = emptyList()
                    }
                    val combined = resultsMap.values.flatten()
                    val dedup = mutableSetOf<String>()
                    _animeList.value = combined.filter {
                        val key = AnimeAggregator.normalizeTitle(it.title)
                        key !in dedup && key.isNotBlank().also { dedup.add(key) }
                    }
                    if (completed == 0 && _animeList.value.isNotEmpty()) _isLoading.value = false
                    completed++
                    if (completed == exts.size) _isLoading.value = false
                }
            }
        }
    }
}
