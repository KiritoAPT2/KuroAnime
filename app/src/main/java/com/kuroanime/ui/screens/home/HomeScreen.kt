package com.kuroanime.ui.screens.home

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuroanime.R
import com.kuroanime.data.model.Anime
import com.kuroanime.ui.components.AnimeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (Anime) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val composeStart = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        Log.d("PERF", "First HomeScreen composition: ${System.currentTimeMillis() - composeStart}ms")
    }

    val animeList by viewModel.animeList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var visibleItems by remember { mutableStateOf(22) }
    LaunchedEffect(selectedCategory) { visibleItems = 22 }

    val currentCategoryLabel = drawerCategories.find { it.id == selectedCategory }?.label ?: "KUROANIME"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category)
                        scope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    isDrawerOpen = drawerState.isOpen
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (selectedCategory == "inicio") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(220.dp, 80.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(0.5f, 0.5f),
                                                radius = 0.8f
                                            )
                                        )
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.logo_kuroanime),
                                    contentDescription = "KUROANIME",
                                    modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(56.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Text(
                                text = currentCategoryLabel,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        val rotation by animateFloatAsState(
                            targetValue = if (drawerState.isOpen) 30f else 0f,
                            animationSpec = tween(durationMillis = 350),
                            label = "menuRotate"
                        )
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(
                                Icons.Default.Menu, contentDescription = "Menú",
                                modifier = Modifier.rotate(rotation)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                if (isLoading && animeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (animeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se pudo cargar nada.\nRevisa tu conexión.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    val displayList = animeList.take(visibleItems)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.url.ifEmpty { it.title } }) { anime ->
                            AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                        }
                        if (visibleItems < animeList.size) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(onClick = { visibleItems += 22 }) {
                                        Text(
                                            "Cargar más (${animeList.size - visibleItems} restantes)",
                                            textDecoration = TextDecoration.Underline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
