package com.kuroanime.ui.screens.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.anilist.CalendarCache
import com.kuroanime.data.anilist.CalendarEntry
import com.kuroanime.data.anilist.CachedSchedule
import com.kuroanime.data.anilist.AniListClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class DayGroup(
    val dayName: String,
    val dayIndex: Int,
    val entries: List<CalendarEntry>
)

class CalendarViewModel : ViewModel() {
    private val _days = MutableStateFlow<List<DayGroup>>(emptyList())
    val days = _days.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated = _lastUpdated.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cached = CalendarCache.get()
            if (!CalendarCache.isExpired() && cached.entries.isNotEmpty()) {
                _days.value = groupByDay(cached.entries)
                _lastUpdated.value = formatTimestamp(cached.lastUpdated)
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            val schedule = withContext(Dispatchers.IO) {
                try {
                    AniListClient.getSchedule()
                } catch (_: Exception) { emptyList() }
            }

            val entries = schedule.mapNotNull { media ->
                val title = media.title?.english ?: media.title?.romaji ?: return@mapNotNull null
                val airing = media.nextAiringEpisode ?: return@mapNotNull null
                val airingAt = airing.airingAt ?: return@mapNotNull null
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = airingAt * 1000 }
                val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 // 0=Lunes..6=Domingo
                CalendarEntry(
                    id = media.id,
                    title = title,
                    imageUrl = media.coverImage?.large,
                    episode = airing.episode,
                    airingAt = airingAt,
                    dayOfWeek = dayOfWeek,
                    season = media.season,
                    seasonYear = media.seasonYear,
                    format = media.format
                )
            }

            val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            val sorted = entries.sortedBy { it.airingAt }
            val scheduleData = CachedSchedule(
                entries = sorted,
                season = schedule.firstOrNull()?.season,
                seasonYear = schedule.firstOrNull()?.seasonYear
            )
            CalendarCache.put(scheduleData)

            _days.value = groupByDay(sorted)
            _lastUpdated.value = formatTimestamp(System.currentTimeMillis())
            _isLoading.value = false
        }
    }

    private fun groupByDay(entries: List<CalendarEntry>): List<DayGroup> {
        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        return entries.groupBy { it.dayOfWeek }
            .map { (dayIndex, list) ->
                DayGroup(dayNames.getOrElse(dayIndex) { "Día $dayIndex" }, dayIndex, list)
            }
            .sortedBy { it.dayIndex }
    }

    private fun formatTimestamp(ms: Long): String {
        if (ms == 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        val df = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return "Actualizado: ${df.format(cal.time)}"
    }
}
