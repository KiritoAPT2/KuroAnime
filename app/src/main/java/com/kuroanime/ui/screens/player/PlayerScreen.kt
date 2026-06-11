package com.kuroanime.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import com.kuroanime.extension.AnimeAggregator
import com.kuroanime.player.EmbedResolver
import com.kuroanime.player.enterImmersive
import com.kuroanime.player.exitImmersive
import com.kuroanime.player.play
import androidx.media3.exoplayer.ExoPlayer
import com.kuroanime.player.rememberExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Suppress("UnsafeOptInUsageError")
private fun createPlayerView(ctx: android.content.Context, player: ExoPlayer) = PlayerView(ctx).also { v ->
    v.player = player
    v.useController = true
    v.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    v.setShowNextButton(false)
    v.setShowPreviousButton(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("InlinedApi")
@Composable
fun PlayerScreen(
    episode: Episode,
    source: String,
    onBack: () -> Unit,
    onOpenWebView: (String) -> Unit
) {
    val player = rememberExoPlayer()
    var sources by remember { mutableStateOf<List<VideoSource>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedDirectSource by remember { mutableStateOf<VideoSource?>(null) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(episode.url) {
        val rawSources = withContext(Dispatchers.IO) {
            AnimeAggregator.getVideoSources(episode.url, source)
        }
        sources = rawSources
        errorMsg = if (rawSources.isEmpty()) "No hay fuentes de video disponibles" else null
    }

    val activity = LocalContext.current as? Activity

    LaunchedEffect(selectedDirectSource) {
        if (selectedDirectSource != null) {
            delay(300)
            activity?.enterImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && selectedDirectSource != null) {
            delay(3000)
            showControls = false
        }
    }

    DisposableEffect(episode.url) {
        onDispose {
            player.stop()
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val isPlaying = selectedDirectSource != null

    Scaffold(
        topBar = {
            if (!isPlaying) {
                TopAppBar(
                    title = { Text("Episodio ${episode.number}") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (isPlaying) it else it.padding(padding) }
            ) {
                if (errorMsg != null && !isPlaying) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMsg!!, textAlign = TextAlign.Center)
                    }
                } else if (isPlaying) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        @Suppress("InlinedApi")
                        AndroidView(
                            factory = { ctx -> createPlayerView(ctx, player) },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (showControls) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .statusBarsPadding(),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                IconButton(onClick = {
                                    selectedDirectSource = null
                                    player.stop()
                                    showControls = true
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        if (showControls) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedDirectSource = null
                                        player.stop()
                                        showControls = true
                                    }
                                ) {
                                    Text("Cambiar servidor", color = Color.White)
                                }
                            }
                        }
                    }
                } else if (sources.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sources) { src ->
                            val isDirect = !EmbedResolver.needsResolve(src.url)
                            ServerCard(
                                server = src.server,
                                isDirect = isDirect,
                                onClick = {
                                    if (isDirect) {
                                        selectedDirectSource = src
                                        player.play(src)
                                    } else {
                                        onOpenWebView(src.url)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: String,
    isDirect: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDirect) Color(0xFF1B5E20) else Color(0xFF0D47A1)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDirect) Icons.Default.PlayArrow else Icons.Default.Language,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (isDirect) "Reproducir directo" else "Abrir en navegador",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
