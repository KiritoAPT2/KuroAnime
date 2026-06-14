package com.kuroanime.ui.screens.animeinfo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode

import com.kuroanime.R
import com.kuroanime.ui.components.KuroCard
import com.kuroanime.ui.components.KuroChip
import com.kuroanime.ui.components.KuroScoreChip
import com.kuroanime.ui.components.KuroTopAppBar
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape
import com.kuroanime.ui.theme.GlassBackground
import com.kuroanime.ui.utils.fadingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeInfoScreen(
    animeUrl: String,
    source: String,
    onEpisodeClick: (Episode, animeTitle: String, animeImage: String) -> Unit,
    onAnimeClick: (Anime) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AnimeInfoViewModel = viewModel(),
) {
    LaunchedEffect(animeUrl) { viewModel.loadAnime(animeUrl, source) }
    val anime by viewModel.anime.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val isRefreshingEpisodes by viewModel.isRefreshingEpisodes.collectAsState()
    val isLoadingEpisodes by viewModel.isLoadingEpisodes.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isNotifying by viewModel.isNotifying.collectAsState()
    val relations by viewModel.relations.collectAsState()
    val enrichedGenres by viewModel.enrichedGenres.collectAsState()
    var loadingRelation by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.toggleNotification()
    }

    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            KuroTopAppBar(
                title = { Text(anime?.title ?: "Información") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@IconButton
                            }
                        }
                        viewModel.toggleNotification()
                    }) {
                        Icon(
                            imageVector = if (isNotifying) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = if (isNotifying) "Desactivar notificaciones" else "Activar notificaciones",
                            tint = if (isNotifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                transparent = true,
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (anime == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val info = anime!!
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullToRefresh(
                        state = pullRefreshState,
                        isRefreshing = isRefreshingEpisodes,
                        onRefresh = { viewModel.reloadEpisodes() },
                    )
                    .fadingEdge(vertical = 32.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    ) {
                        if (!info.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(info.imageUrl)
                                    .crossfade(true)
                                    .size(640)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF000000).copy(alpha = 0.25f),
                                            Color(0xFF000000).copy(alpha = 0.95f),
                                        ),
                                    ),
                                ),
                        )
                    }
                }

                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = KuroDimens.spacingMd),
                        ) {
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = info.title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(Modifier.height(KuroDimens.spacingSm))

                            Row(horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingXs)) {
                                if (!info.status.isNullOrBlank()) {
                                    KuroChip(text = info.status, selected = true, onClick = {})
                                }
                                if (!info.type.isNullOrBlank()) {
                                    KuroChip(text = info.type, selected = false, onClick = {})
                                }
                                if (!info.score.isNullOrBlank()) {
                                    KuroScoreChip(score = info.score)
                                }
                                if (info.audio != null) {
                                    KuroChip(text = info.audio, selected = false, onClick = {})
                                }
                            }

                            val displayGenres = enrichedGenres.ifEmpty { info.genres }
                            if (displayGenres.isNotEmpty()) {
                                Spacer(Modifier.height(KuroDimens.spacingXs))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    displayGenres.take(8).forEach { genre ->
                                        KuroChip(text = genre, selected = false, onClick = { onGenreClick(genre) })
                                    }
                                }
                            }

                            if (!info.synopsis.isNullOrBlank()) {
                                Spacer(Modifier.height(KuroDimens.spacingLg))
                                Text(
                                    "Sinopsis",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    info.synopsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                )
                            }

                            Spacer(Modifier.height(KuroDimens.spacingLg))
                            Text(
                                "Episodios",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(KuroDimens.spacingXs))
                        }
                    }

                    if (isLoadingEpisodes && episodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        }
                    }

                    items(episodes) { ep ->
                        KuroCard(
                            onClick = { onEpisodeClick(ep, info.title, info.imageUrl ?: "") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = KuroDimens.spacingMd, vertical = 3.dp),
                            shape = KuroShape.medium,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            elevation = 0.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = KuroDimens.spacingSm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    ep.number.toString().padStart(2, '0'),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(36.dp),
                                )
                                Spacer(Modifier.width(KuroDimens.spacingSm))
                                Text(
                                    ep.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "▶",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }

                    if (relations.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(KuroDimens.spacingXs))
                            Text(
                                "Relacionados",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = KuroDimens.spacingMd),
                            )
                            Spacer(Modifier.height(KuroDimens.spacingSm))
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = KuroDimens.spacingMd),
                                horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
                            ) {
                                items(relations, key = { it.anilistId }) { rel ->
                                    val label = rel.relationType.lowercase().replace('_', ' ')
                                        .replaceFirstChar { it.uppercase() }
                                    val isLoading = loadingRelation == rel.title
                                    KuroCard(
                                        onClick = {
                                            if (!isLoading) {
                                                loadingRelation = rel.title
                                                viewModel.searchAnimeByTitle(rel.title) { animeResult ->
                                                    loadingRelation = null
                                                    if (animeResult != null) onAnimeClick(animeResult)
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(130.dp),
                                        shape = KuroShape.medium,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        elevation = 0.dp,
                                    ) {
                                        Box {
                                            Column {
                                                if (rel.imageUrl != null) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(rel.imageUrl)
                                                            .crossfade(true)
                                                            .size(260, 340)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(KuroDimens.cardAspectRatio),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(170.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                    )
                                                }
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        rel.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                            if (isLoading) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF000000).copy(alpha = 0.6f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
            }
    }
}

                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(KuroDimens.spacingMd)) }
                }
            }
        }
    }
}


