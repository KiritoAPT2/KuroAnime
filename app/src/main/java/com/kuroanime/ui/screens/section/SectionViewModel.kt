package com.kuroanime.ui.screens.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val CACHE_TTL = 900_000L // 15 min

class SectionViewModel : ViewModel() {
    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    fun load(sectionId: String, source: String) {
        val cacheKey = "section_${sectionId}_${source}"
        val cached = ResultCache.get<List<Anime>>(cacheKey)
        if (cached != null) {
            _animeList.value = cached
            _isLoading.value = false
            return
        }

        val sectionTitle = when (sectionId) {
            "latest" -> "Últimos episodios"
            "tendencias" -> "Tendencias"
            "accion" -> "🔥 Acción"
            "romance" -> "💕 Romance"
            "comedia" -> "🤣 Comedia"
            else -> sectionId.replaceFirstChar { it.uppercase() }
        }
        _title.value = sectionTitle

        viewModelScope.launch {
            _isLoading.value = true
            val results = withContext(Dispatchers.IO) {
                try {
                    when (sectionId) {
                        "tendencias" -> {
                            withTimeout(5000) {
                                ExtensionManager.getByName("TioAnime")?.getLatest(1)?.take(40) ?: emptyList()
                            }
                        }
                        "latest" -> {
                            withTimeout(8000) {
                                ExtensionManager.getByName("AnimeFLV")?.getLatest(1)?.take(40) ?: emptyList()
                            }
                        }
                        else -> {
                            withTimeout(8000) {
                                ExtensionManager.getByName(source)?.getByGenre(sectionId, 1)?.take(40) ?: emptyList()
                            }
                        }
                    }
                } catch (_: Exception) { emptyList() }
            }
            ResultCache.set(cacheKey, results, CACHE_TTL)
            _animeList.value = results
            _isLoading.value = false
        }
    }
}
