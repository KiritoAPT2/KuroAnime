package com.kuroanime.ui.screens.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.FavoriteAnime
import com.kuroanime.ui.components.AnimeCard
import com.kuroanime.ui.utils.fadingEdge

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onAnimeClick: (FavoriteAnime) -> Unit,
    onBack: () -> Unit = {},
    viewModel: FavoritesViewModel = viewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    var itemToRemove by remember { mutableStateOf<FavoriteAnime?>(null) }

    if (itemToRemove != null) {
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("Quitar de favoritos") },
            text = { Text("¿Quitar \"${itemToRemove!!.title}\" de tus favoritos?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(itemToRemove!!.animeId)
                    itemToRemove = null
                }) {
                    Text("Quitar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRemove = null }) { Text("Cancelar") }
            },
        )
    }

    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (favorites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "♡",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No tienes favoritos aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Text(
                            "Explora y agrega animes a tu lista",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = TopAppBarDefaults.TopAppBarExpandedHeight,
                        bottom = 80.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(vertical = 32.dp),
                ) {
                    items(favorites, key = { it.animeId }) { fav ->
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = { onAnimeClick(fav) },
                                onLongClick = { itemToRemove = fav },
                            ),
                        ) {
                            AnimeCard(
                                anime = Anime(
                                    title = fav.title,
                                    url = fav.animeId,
                                    imageUrl = fav.imageUrl,
                                    source = fav.source,
                                ),
                                onClick = {},
                            )
                        }
                    }
                }
            }
    }
}
