package com.kuroanime.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import com.kuroanime.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuroanime.data.SettingsManager
import com.kuroanime.data.VideoSourceCache
import com.kuroanime.data.local.ContinueWatchingStorage
import com.kuroanime.data.local.HistoryStorage
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.HistoryEntry
import com.kuroanime.data.model.VideoSource
import com.kuroanime.extension.AnimeAggregator
import com.kuroanime.extractor.ProPlayerEngine
import com.kuroanime.extractor.ServerHealth
import com.kuroanime.player.EmbedResolver
import com.kuroanime.player.WebViewResolver
import com.kuroanime.player.enterImmersive
import androidx.media3.common.Player
import com.kuroanime.player.exitImmersive
import com.kuroanime.player.play
import com.kuroanime.player.rememberExoPlayer
import com.kuroanime.ui.components.KuroBottomSheet
import com.kuroanime.ui.components.KuroCard
import com.kuroanime.ui.components.KuroChip
import com.kuroanime.ui.components.KuroSeekBar
import com.kuroanime.ui.components.KuroSheetItem
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private fun rankSources(sources: List<VideoSource>): List<VideoSource> {
    val knownOrder = listOf(
        "streamtape", "stape", "yourupload", "dsvplay",
        "netu", "mp4upload", "hexload", "savefiles",
        "voe", "mixdrop", "mega", "byse"
    )
    return sources.sortedBy { src ->
        val baseIdx = knownOrder.indexOfFirst { src.server.contains(it, ignoreCase = true) }
        val directPenalty = if (!EmbedResolver.needsResolve(src.url)) 0 else 10000
        val latencyBonus = ServerHealth.getAverageLatency(src.server).coerceIn(0, 5000) / 100
        (if (baseIdx >= 0) baseIdx else knownOrder.size) + directPenalty + latencyBonus
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("InlinedApi")
@Composable
fun PlayerScreen(
    episode: Episode,
    source: String,
    onBack: () -> Unit,
    onOpenWebView: (String) -> Unit,
    onNextEpisode: ((Episode) -> Unit)? = null,
    animeUrl: String = "",
    animeTitle: String = "Episodio ${episode.number}",
    animeImage: String = "",
    episodes: List<Episode> = emptyList()
) {
    val player = rememberExoPlayer()
    val activity = LocalContext.current as? Activity

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sources by remember { mutableStateOf<List<VideoSource>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember { mutableStateOf<VideoSource?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    var autoplayNext by remember { mutableStateOf(SettingsManager.getAutoplay()) }
    var seekOverlayText by remember { mutableStateOf<String?>(null) }
    var playWhenReady by remember { mutableStateOf(true) }

    var showResumeDialog by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var autoPlayFailed by remember { mutableStateOf(false) }
    val img = animeImage.ifBlank { null }

    val selectSource: (VideoSource) -> Unit = { src ->
        autoPlayFailed = false
        if (!EmbedResolver.needsResolve(src.url)) {
            selectedSource = src
            player.play(src)
        } else {
            isResolving = true
            showControls = true
            scope.launch {
                val urls = WebViewResolver.resolve(context, src.url)
                isResolving = false
                if (urls.isNotEmpty()) {
                    val resolvedSrc = src.copy(url = urls.first())
                    selectedSource = resolvedSrc
                    player.play(resolvedSrc)
                } else {
                    onOpenWebView(src.url)
                }
            }
        }
    }

    LaunchedEffect(episode.url) {
        val rawSources = withContext(Dispatchers.IO) {
            AnimeAggregator.getVideoSources(episode.url, source)
        }
        sources = rawSources
        errorMsg = if (rawSources.isEmpty()) "No hay fuentes de video disponibles" else null
        autoPlayFailed = false
        if (rawSources.isNotEmpty()) {
            val saved = ContinueWatchingStorage.getAll()
                .find { it.animeId == animeUrl && it.episodeNumber == episode.number }
            val best = if (saved != null && saved.server.isNotBlank()) {
                rawSources.find { it.server == saved.server }
                    ?: rankSources(rawSources).firstOrNull()
            } else {
                rankSources(rawSources).firstOrNull()
            }
            if (best != null) {
                selectSource(best)
            }
        }
if (rawSources.isNotEmpty() && episodes.isNotEmpty()) {
    withContext(Dispatchers.IO) {
        ProPlayerEngine.preloadNext(episode.url, source, episodes)
    }
}
    }

    val isPlaying = selectedSource != null

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(300)
            activity?.enterImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            isFullscreen = true
        } else {
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val existing = ContinueWatchingStorage.getAll()
            .find { it.animeId == animeUrl && it.episodeNumber == episode.number }
        if (existing != null && existing.positionMs > 5000) {
            resumePosition = existing.positionMs
            showResumeDialog = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        HistoryStorage.add(HistoryEntry(
            animeId = animeUrl,
            animeTitle = animeTitle,
            animeImage = animeImage.ifBlank { null },
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            source = source,
            lastWatchedAt = System.currentTimeMillis()
        ))
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            delay(300)
            currentPosition = player.currentPosition
            totalDuration = player.duration.coerceAtLeast(0L)
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            delay(15_000)
            val pos = player.currentPosition
            if (pos > 5000) {
                ContinueWatchingStorage.save(ContinueWatching(
                    animeId = animeUrl,
                    animeTitle = animeTitle,
                    animeImage = img,
                    episodeNumber = episode.number,
                    episodeTitle = episode.title,
                    episodeUrl = episode.url,
                    source = source,
                    positionMs = pos,
                    durationMs = player.duration.coerceAtLeast(pos)
                ))
            }
        }
    }

    var hasTriggeredAutoplay by remember { mutableStateOf(false) }
    val onNext = onNextEpisode

    LaunchedEffect(isPlaying, autoplayNext, onNext) {
        if (!isPlaying || !autoplayNext || onNext == null) return@LaunchedEffect
        hasTriggeredAutoplay = false
        while (true) {
            delay(1000)
            val dur = player.duration.coerceAtLeast(0L)
            val pos = player.currentPosition
            if (dur > 30_000 && pos >= dur - 10_000 && !hasTriggeredAutoplay) {
                hasTriggeredAutoplay = true
                val nextEpNum = episode.number + 1
                val nextEp = Episode(
                    number = nextEpNum,
                    title = "Episodio $nextEpNum",
                    url = "",
                )
                onNext(nextEp)
            }
        }
    }

    LaunchedEffect(selectedSource) {
        if (selectedSource != null && selectedSource!!.server.isNotBlank()) {
            val existing = ContinueWatchingStorage.getAll()
                .find { it.animeId == animeUrl && it.episodeNumber == episode.number }
            if (existing != null && existing.server != selectedSource!!.server) {
                ContinueWatchingStorage.save(existing.copy(server = selectedSource!!.server))
            }
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            delay(100)
            playWhenReady = player.playWhenReady
        }
    }

    DisposableEffect(Unit) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                VideoSourceCache.invalidateForEpisode(episode.url, source)
                if (selectedSource != null && !autoPlayFailed) {
                    autoPlayFailed = true
                    selectedSource = null
                }
            }
        }
        player.addListener(errorListener)
        onDispose {
            player.removeListener(errorListener)
            try {
                val pos = player.currentPosition
                if (pos > 5000) {
                    runBlocking {
                        ContinueWatchingStorage.save(ContinueWatching(
                            animeId = animeUrl,
                            animeTitle = animeTitle,
                            animeImage = img,
                            episodeNumber = episode.number,
                            episodeTitle = episode.title,
                            episodeUrl = episode.url,
                            source = source,
                            positionMs = pos,
                            durationMs = player.duration.coerceAtLeast(pos)
                        ))
                    }
                }
                player.stop()
            } catch (_: Exception) { }
            try { player.release() } catch (_: Exception) { }
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (seekOverlayText != null) {
        LaunchedEffect(seekOverlayText) {
            delay(600)
            seekOverlayText = null
        }
    }

    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Continuar reproducción") },
            text = { Text("¿Continuar desde ${formatTime(resumePosition)}?") },
            confirmButton = {
                TextButton(onClick = {
                    player.seekTo(resumePosition)
                    showResumeDialog = false
                }) { Text("Continuar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showResumeDialog = false }) { Text("Empezar desde 0") }
            }
        )
    }

    if (showServerSheet) {
        KuroBottomSheet(
            onDismissRequest = { showServerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            title = "Seleccionar servidor",
        ) {
            sources.forEach { src ->
                val isDirect = !EmbedResolver.needsResolve(src.url)
                KuroSheetItem(
                    text = src.server.uppercase(),
                    onClick = {
                        showServerSheet = false
                        player.stop()
                        selectSource(src)
                    },
                    selected = src.url == selectedSource?.url,
                    trailingContent = {
                        Text(
                            if (isDirect) "Directo" else "Resolver",
                            color = if (isDirect) Color(0xFF4CAF50) else Color(0xFF90CAF9),
                            fontSize = 11.sp,
                        )
                    },
                )
                if (src != sources.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showSettingsSheet) {
        KuroBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            title = "Ajustes",
        ) {
            Text(
                "Velocidad",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(KuroDimens.spacingXs))
            val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                speeds.forEachIndexed { i, speed ->
                    KuroChip(
                        text = if (speed == 1f) "Normal" else "${speed}x",
                        selected = playbackSpeed == speed,
                        onClick = { playbackSpeed = speed; player.setPlaybackSpeed(speed) },
                    )
                    if (i < speeds.lastIndex) Spacer(Modifier.width(KuroDimens.spacingXs))
                }
            }

            Spacer(Modifier.height(KuroDimens.spacingLg))

            Text(
                "Zoom",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(KuroDimens.spacingXs))
            val zoomModes = listOf(
                AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
                AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                zoomModes.forEachIndexed { i, (mode, label) ->
                    KuroChip(
                        text = label,
                        selected = resizeMode == mode,
                        onClick = { resizeMode = mode },
                    )
                    if (i < zoomModes.lastIndex) Spacer(Modifier.width(KuroDimens.spacingXs))
                }
            }

            Spacer(Modifier.height(KuroDimens.spacingLg))

            Text(
                "Calidad",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Auto",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(KuroDimens.spacingLg))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(Modifier.height(KuroDimens.spacingMd))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Auto-siguiente",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Switch(
                    checked = autoplayNext,
                    onCheckedChange = { checked ->
                        autoplayNext = checked
                        SettingsManager.setAutoplay(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ),
                )
            }
            Spacer(Modifier.height(KuroDimens.spacingXl))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isPlaying) {
            PlayingContent(
                player = player,
                animeTitle = animeTitle,
                showControls = showControls,
                playWhenReady = playWhenReady,
                isFullscreen = isFullscreen,
                seekOverlayText = seekOverlayText,
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                onToggleControls = { showControls = !showControls },
                onBack = onBack,
                onOpenServerSheet = { showServerSheet = true },
                onSettingsClick = { showSettingsSheet = true },
                onFullscreenClick = {
                    isFullscreen = !isFullscreen
                    if (isFullscreen) { activity?.enterImmersive(); activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE }
                    else { activity?.exitImmersive(); activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                },
                onPlayPauseClick = { player.playWhenReady = !player.playWhenReady },
                onSeek = { pos -> player.seekTo(pos) },
                onSeekBack = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0L)) },
                onSeekForward = { player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration.coerceAtLeast(0L))) },
                onSeekOverlay = { seekOverlayText = it }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f),
                    ) {
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Text("←", color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        animeTitle.take(40),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                    )
                }
                when {
                    errorMsg != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(errorMsg!!, textAlign = TextAlign.Center, color = Color.White)
                        }
                    }
                    isResolving -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(KuroDimens.spacingSm))
                                Text("Conectando...", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    autoPlayFailed && sources.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(KuroDimens.spacingMd),
                            verticalArrangement = Arrangement.spacedBy(KuroDimens.spacingXs),
                        ) {
                            items(sources, key = { it.url }) { src ->
                                val isDirect = !EmbedResolver.needsResolve(src.url)
                                ServerCard(server = src.server, isDirect = isDirect, onClick = { selectSource(src) })
                            }
                        }
                    }
                    sources.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingContent(
    player: ExoPlayer,
    animeTitle: String,
    showControls: Boolean,
    playWhenReady: Boolean,
    isFullscreen: Boolean,
    seekOverlayText: String?,
    currentPosition: Long,
    totalDuration: Long,
    onToggleControls: () -> Unit,
    onBack: () -> Unit,
    onOpenServerSheet: () -> Unit,
    onSettingsClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekOverlay: (String?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also { v ->
                    v.player = player
                    v.useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) { onSeekBack(); onSeekOverlay("-10s") }
                            else { onSeekForward(); onSeekOverlay("+10s") }
                        }
                    )
                }
        )

        if (seekOverlayText != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(seekOverlayText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        if (!playWhenReady && !showControls) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Reanudar",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize().padding(14.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            ControlsOverlay(
                animeTitle = animeTitle,
                isFullscreen = isFullscreen,
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                playWhenReady = playWhenReady,
                onBack = onBack,
                onOpenServerSheet = onOpenServerSheet,
                onSettingsClick = onSettingsClick,
                onFullscreenClick = onFullscreenClick,
                onPlayPauseClick = onPlayPauseClick,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
                onSeek = onSeek
            )
        }
    }
}

@Composable
private fun ControlsOverlay(
    animeTitle: String,
    isFullscreen: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    playWhenReady: Boolean,
    onBack: () -> Unit,
    onOpenServerSheet: () -> Unit,
    onSettingsClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.5f), Color.Transparent))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f)))),
        )

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    animeTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            IconButton(onClick = onSeekBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Replay10, "Retroceder 10s", tint = Color.White.copy(0.7f), modifier = Modifier.size(24.dp))
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                onClick = onPlayPauseClick,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    if (playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (playWhenReady) "Pausar" else "Reanudar",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                )
            }
            IconButton(onClick = onSeekForward, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Forward10, "Adelantar 10s", tint = Color.White.copy(0.7f), modifier = Modifier.size(24.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = KuroDimens.spacingSm, vertical = 4.dp),
        ) {
            KuroSeekBar(
                currentPositionMs = currentPosition,
                durationMs = totalDuration,
                onSeek = onSeek,
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onOpenServerSheet, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Dns, "Servidor", tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, "Ajustes", tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onFullscreenClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            "Pantalla completa",
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(server: String, isDirect: Boolean, onClick: () -> Unit) {
    KuroCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = KuroShape.medium,
        containerColor = if (isDirect) Color(0xFF8B0000) else Color(0xFF4A0E0E),
        elevation = KuroDimens.elevationSm,
    ) {
        Row(
            Modifier.padding(KuroDimens.spacingMd).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isDirect) Icons.Default.PlayArrow else Icons.Default.Language,
                null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(KuroDimens.spacingSm))
            Column(Modifier.weight(1f)) {
                Text(server.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (isDirect) "Reproducir directo" else "Resolver y reproducir",
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

