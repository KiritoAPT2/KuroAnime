package com.kuroanime.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.ui.components.AnimeCard
import com.kuroanime.ui.components.KuroCard
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape
import com.kuroanime.ui.utils.fadingEdge

@Composable
fun ExploreScreen(
    onAnimeClick: (Anime) -> Unit,
    onPeliculasClick: () -> Unit = {},
    onCalendarioClick: () -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    viewModel: ExploreViewModel = viewModel(),
) {
    val trending by viewModel.trending.collectAsState()

    val lazyListState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .fadingEdge(vertical = 32.dp),
                contentPadding = PaddingValues(top = 56.dp, bottom = 80.dp),
            ) {
                if (trending.isNotEmpty()) {
                    item { SectionTitle("Tendencias") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = KuroDimens.spacingMd),
                            horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingMd),
                        ) {
                            items(trending, key = { it.url }) { anime ->
                                AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                            }
                        }
                    }
                }

                item { SectionTitle("Temporada actual") }
                item { SeasonTile(onClick = onCalendarioClick) }

                item { SectionTitle("Películas") }
                item { MoviesTile(onClick = onPeliculasClick) }

                item { SectionTitle("Géneros") }
                item {
                    GenreGrid(
                        genres = ExploreViewModel.genreChips,
                        onClick = onGenreClick,
                    )
                }

                item { Spacer(Modifier.height(KuroDimens.spacingMd)) }
            }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            start = KuroDimens.spacingMd,
            top = 28.dp,
            bottom = KuroDimens.spacingSm,
        ),
    )
}

@Composable
private fun ExploreContinueWatchingRow(
    items: List<ContinueWatching>,
    onItemClick: (ContinueWatching) -> Unit,
) {
    val ctx = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = KuroDimens.spacingMd),
        horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
    ) {
        items(items, key = { "${it.animeId}_${it.episodeNumber}" }) { cw ->
            val progress = if (cw.durationMs > 0) (cw.positionMs.toFloat() / cw.durationMs).coerceIn(0f, 1f) else 0f
            val pct = (progress * 100).toInt()
            KuroCard(
                onClick = { onItemClick(cw) },
                modifier = Modifier.width(180.dp),
                shape = KuroShape.medium,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    ) {
                        if (cw.animeImage != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(cw.animeImage)
                                    .crossfade(true)
                                    .size(360, 540)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(KuroShape.medium),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) { Text("", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                    ),
                                ),
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = KuroDimens.spacingSm, vertical = KuroDimens.spacingXs),
                        ) {
                            Text(
                                cw.animeTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ep. ${cw.episodeNumber} - $pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            .clip(RoundedCornerShape(bottomStart = KuroDimens.spacingSm, bottomEnd = KuroDimens.spacingSm)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonTile(onClick: () -> Unit) {
    KuroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroDimens.spacingMd),
        shape = KuroShape.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ver calendario de emisión →",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun MoviesTile(onClick: () -> Unit) {
    KuroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroDimens.spacingMd),
        shape = KuroShape.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Explorar películas →",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun GenreGrid(
    genres: List<String>,
    onClick: (String) -> Unit,
) {
    val chunked = genres.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = KuroDimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
    ) {
        chunked.forEach { row ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
            ) {
                row.forEach { genre ->
                    KuroCard(
                        onClick = { onClick(genre) },
                        modifier = Modifier.weight(1f),
                        shape = KuroShape.medium,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
                if (row.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
