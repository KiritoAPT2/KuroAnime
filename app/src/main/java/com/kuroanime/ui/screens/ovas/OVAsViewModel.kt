package com.kuroanime.ui.screens.ovas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.AnimeFlvExtension
import com.kuroanime.extension.ExtensionManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val CACHE_KEY = "ovas"
private const val CACHE_TTL = 7 * 24 * 60 * 60 * 1000L // 7 días

private val ovasJson = Json { ignoreUnknownKeys = true }

class OVAsViewModel : ViewModel() {
    private val _ovas = MutableStateFlow<List<Anime>>(emptyList())
    val ovas = _ovas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        val cached = ResultCache.get<List<Anime>>(CACHE_KEY)
        if (cached != null) {
            _ovas.value = cached
            _isLoading.value = false
        } else {
            val persisted = PersistentResultCache.getString(CACHE_KEY)
            if (persisted != null) {
                try {
                    val parsed = ovasJson.decodeFromString(ListSerializer(Anime.serializer()), persisted)
                    ResultCache.set(CACHE_KEY, parsed, CACHE_TTL)
                    _ovas.value = parsed
                    _isLoading.value = false
                } catch (_: Exception) {
                    load()
                }
            } else {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val flv = ExtensionManager.getByName("AnimeFLV")
            val results = withContext(Dispatchers.IO) {
                try {
                    withTimeout(10000) {
                        (flv as? AnimeFlvExtension)?.getByType("ova", 1)?.take(40) ?: emptyList()
                    }
                } catch (_: Exception) { emptyList() }
            }
            if (results.isNotEmpty()) {
                ResultCache.set(CACHE_KEY, results, CACHE_TTL)
                try { PersistentResultCache.setString(CACHE_KEY, ovasJson.encodeToString(ListSerializer(Anime.serializer()), results), CACHE_TTL) } catch (_: Exception) {}
            }
            _ovas.value = results
            _isLoading.value = false
        }
    }
}
