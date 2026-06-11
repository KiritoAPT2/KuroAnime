package com.kuroanime.player

import android.net.Uri
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.kuroanime.data.model.VideoSource

@Composable
fun rememberExoPlayer(): ExoPlayer {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        ExoPlayer.Builder(context).build()
    }.also { player ->
        DisposableEffect(player) {
            onDispose { player.release() }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun ExoPlayer.play(source: VideoSource) {
    val uri = Uri.parse(source.url)
    val headers = source.headers ?: emptyMap()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(headers)
        .setAllowCrossProtocolRedirects(true)
    val mediaSource: MediaSource = if (source.url.contains(".m3u8")) {
        HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    } else {
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    }
    setMediaSource(mediaSource)
    prepare()
    playWhenReady = true
}
