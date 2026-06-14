package com.kuroanime.ui.screens.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.extension.AnimeAggregator
import com.kuroanime.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val CACHE_TTL = 7 * 24 * 60 * 60 * 1000L // 7 días

private val genreMap = mapOf(
    "Acción" to "accion", "Artes Marciales" to "artes-marciales",
    "Aventuras" to "aventura", "Carreras" to "carreras",
    "Ciencia Ficción" to "ciencia-ficcion", "Comedia" to "comedia",
    "Demencia" to "demencia", "Demonios" to "demonios",
    "Deportes" to "deportes", "Drama" to "drama",
    "Ecchi" to "ecchi", "Escolares" to "escolares",
    "Espacial" to "espacial", "Fantasía" to "fantasia",
    "Harem" to "harem", "Historico" to "historico",
    "Infantil" to "infantil", "Josei" to "josei",
    "Juegos" to "juegos", "Magia" to "magia",
    "Mecha" to "mecha", "Militar" to "militar",
    "Misterio" to "misterio", "Música" to "musica",
    "Parodia" to "parodia", "Policía" to "policia",
    "Psicológico" to "psicologico", "Recuentos de la vida" to "recuentos-de-la-vida",
    "Romance" to "romance", "Samurai" to "samurai",
    "Seinen" to "seinen", "Shoujo" to "shoujo",
    "Shounen" to "shounen", "Sobrenatural" to "sobrenatural",
    "Superpoderes" to "superpoderes", "Suspenso" to "suspenso",
    "Terror" to "terror", "Vampiros" to "vampiros",
    "Yaoi" to "yaoi", "Yuri" to "yuri"
)

class GenreViewModel : ViewModel() {
    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    fun loadGenre(genre: String, source: String) {
        val cacheKey = "genre_${genre.lowercase()}_${source.lowercase()}"
        val cached = ResultCache.get<List<Anime>>(cacheKey)
        if (cached != null) {
            _animeList.value = cached
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _animeList.value = emptyList()

            val genreSlug = genreMap[genre] ?: genre.lowercase().replace(' ', '-')
            val genreLower = genre.lowercase()

            if (source == "Latanime" && genreLower == "latino") {
                loadFromLatanime()
            } else if (source == "AnimeFLV" || source == "TioAnime") {
                loadFromAnimeFLV(genreSlug)
                loadFromTioAnime(genreSlug)
            } else {
                loadFallback(genreSlug)
            }

            ResultCache.set(cacheKey, _animeList.value, CACHE_TTL)
            _isLoading.value = false
        }
    }

    private suspend fun loadFromLatanime() {
        try {
            val results = withContext(Dispatchers.IO) {
                withTimeout(8000) {
                    ExtensionManager.getByName("Latanime")?.getByGenre("latino", 1)?.take(30) ?: emptyList()
                }
            }
            if (results.isNotEmpty()) _animeList.value = results
        } catch (_: Exception) {}
    }

    private suspend fun loadFromAnimeFLV(genreSlug: String) {
        try {
            val flv = withContext(Dispatchers.IO) {
                withTimeout(8000) { ExtensionManager.getByName("AnimeFLV")?.getByGenre(genreSlug, 1)?.take(24) ?: emptyList() }
            }
            if (flv.isNotEmpty()) _animeList.value = flv
        } catch (_: Exception) {}
    }

    private suspend fun loadFromTioAnime(genreSlug: String) {
        try {
            val tio = withContext(Dispatchers.IO) {
                withTimeout(4000) { ExtensionManager.getByName("TioAnime")?.getByGenre(genreSlug, 1)?.take(24) ?: emptyList() }
            }
            if (tio.isNotEmpty()) {
                val merged = AnimeAggregator.deduplicate(_animeList.value + tio).take(24)
                _animeList.value = merged
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadFallback(genreSlug: String) {
        try {
            val results = withContext(Dispatchers.IO) {
                AnimeAggregator.search(genreSlug.replace('-', ' ')).take(24)
            }
            if (results.isNotEmpty()) _animeList.value = results
        } catch (_: Exception) {}
    }
}
