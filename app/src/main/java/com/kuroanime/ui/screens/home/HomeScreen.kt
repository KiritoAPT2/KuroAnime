package com.kuroanime.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.ui.components.AnimeCard
import com.kuroanime.ui.components.AuroraBackground
import com.kuroanime.ui.components.HomeSkeletonLoading
import com.kuroanime.ui.components.TopActionChips
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.utils.fadingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    dynamicColor: Boolean = false,
    onAnimeClick: (Anime) -> Unit,
    onContinueWatchingClick: (ContinueWatching) -> Unit = {},
    onLatestEpisodeClick: (LatestEpisode) -> Unit = {},
    onLatestSectionClick: () -> Unit = {},
    onSectionClick: (String, String) -> Unit = { _, _ -> },
    onPeliculasClick: () -> Unit = {},
    onOVAsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onNewsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val continueWatching by viewModel.continueWatching.collectAsState()
    val latestEpisodes by viewModel.latestEpisodes.collectAsState()
    val popularAnime by viewModel.popularAnime.collectAsState()
    val recommendedAnime by viewModel.recommendedAnime.collectAsState()
    val generosPopulares by viewModel.generosPopulares.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.randomAnimeEvent.collect { anime ->
            onAnimeClick(anime)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadContinueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    generosPopulares.forEach { genreSection ->
        if (genreSection.items.isEmpty() && !genreSection.isLoading) {
            val layoutInfo by remember { derivedStateOf { lazyListState.layoutInfo } }
            val lastVisibleItem by remember { derivedStateOf { layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
            val genreIndex = generosPopulares.indexOf(genreSection)
            val sectionIndex = 7 + genreIndex
            LaunchedEffect(lastVisibleItem, sectionIndex) {
                if (lastVisibleItem >= sectionIndex - 1) {
                    viewModel.loadGenero(genreSection.genre)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(
            isVisible = true,
            dynamicColor = dynamicColor,
            scrollOffsetProvider = {
                if (lazyListState.firstVisibleItemIndex > 0) 1000f
                else lazyListState.firstVisibleItemScrollOffset.toFloat()
            },
        ) {
            if (isLoading && latestEpisodes.isEmpty() && popularAnime.isEmpty()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().fadingEdge(vertical = 32.dp),
                ) {
                    item { HomeSkeletonLoading() }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pullToRefresh(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = viewModel::refresh,
                        )
                        .fadingEdge(vertical = 32.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    item { Spacer(Modifier.height(63.dp)) }

                    item {
                        TopActionChips(
                            onChipClick = { value ->
                                when (value) {
                                    "history" -> onHistoryClick()
                                    "peliculas" -> onPeliculasClick()
                                    "ovas" -> onOVAsClick()
                                    "random" -> viewModel.loadRandomAnime()
                                }
                            },
                        )
                    }

                    if (latestEpisodes.isNotEmpty()) {
                        item {
                            LatestEpisodeRow(
                                episodes = latestEpisodes,
                                onItemClick = onLatestEpisodeClick,
                                onTitleClick = onLatestSectionClick,
                            )
                        }
                    }

                    if (continueWatching.isNotEmpty()) {
                        item {
                            ContinueWatchingRow(
                                items = continueWatching,
                                onItemClick = onContinueWatchingClick,
                                onRemoveItem = { viewModel.removeContinueWatching(it.animeId, it.episodeNumber) },
                            )
                        }
                    }

                    if (popularAnime.isNotEmpty()) {
                        item { AnimeHorizontalSection(title = "Populares", items = popularAnime, onItemClick = onAnimeClick) }
                    }

                    if (recommendedAnime.isNotEmpty()) {
                        item { AnimeHorizontalSection(title = "Para ti", items = recommendedAnime, onItemClick = onAnimeClick) }
                    }

                    generosPopulares.forEach { genreSection ->
                        item(key = "genero_${genreSection.genre}") {
                            GenreSectionView(
                                title = genreSection.title,
                                items = genreSection.items,
                                isLoading = genreSection.isLoading,
                                onItemClick = onAnimeClick,
                            )
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GenreSectionView(
    title: String,
    items: List<Anime>,
    isLoading: Boolean,
    onItemClick: (Anime) -> Unit,
) {
    if (items.isEmpty() && !isLoading) return
    Box(Modifier.fillMaxWidth()) {
        Column {
            Spacer(Modifier.height(KuroDimens.spacingLg))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = KuroDimens.spacingMd, vertical = 8.dp),
            )
            if (isLoading && items.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(24.dp),
                    )
                }
            } else if (items.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = KuroDimens.spacingMd),
                    horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
                ) {
                    items(items, key = { it.url }) { anime ->
                        AnimeCard(anime = anime, onClick = { onItemClick(anime) })
                    }
                }
            }
        }
    }
}
