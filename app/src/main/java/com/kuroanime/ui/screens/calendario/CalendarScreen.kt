package com.kuroanime.ui.screens.calendario

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuroanime.data.anilist.CalendarEntry
import com.kuroanime.data.model.Anime
import com.kuroanime.ui.components.KuroCard
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape
import com.kuroanime.ui.utils.fadingEdge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    onAnimeClick: (Anime) -> Unit,
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = viewModel(),
) {
    val days by viewModel.days.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()

    val lazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (days.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay datos del calendario",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(vertical = 32.dp),
                    contentPadding = PaddingValues(top = TopAppBarDefaults.TopAppBarExpandedHeight, bottom = 80.dp),
                ) {
                    days.forEach { day ->
                        item {
                            Text(
                                day.dayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = KuroDimens.spacingMd,
                                    top = 24.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                        items(day.entries, key = { it.id }) { entry ->
                            CalendarRow(entry = entry, onClick = {
                                onAnimeClick(
                                    Anime(
                                        title = entry.title,
                                        url = "",
                                        imageUrl = entry.imageUrl ?: "",
                                        source = "AnimeFLV",
                                    ),
                                )
                            })
                        }
                    }
                    if (lastUpdated.isNotBlank()) {
                        item {
                            Text(
                                lastUpdated,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 16.dp,
                                    bottom = 8.dp,
                                ),
                            )
            }
    }
}

        }
    }
}

@Composable
private fun CalendarRow(entry: CalendarEntry, onClick: () -> Unit) {
    KuroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroDimens.spacingMd, vertical = 4.dp),
        shape = KuroShape.small,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                entry.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            )
            val epText = entry.episode?.let { "Episodio $it" } ?: ""
            val timeText = entry.airingAt?.let { formatAiringTime(it) } ?: ""
            val meta = listOfNotNull(epText, timeText).joinToString(" • ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    meta,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatAiringTime(airingAt: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = airingAt - now
    return when {
        diff <= 0 -> "Emisión"
        diff < 3600 -> "En ${diff / 60} min"
        diff < 86400 -> "En ${diff / 3600}h"
        diff < 172800 -> "Mañana"
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = airingAt * 1000 }
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(cal.time)
        }
    }
}
