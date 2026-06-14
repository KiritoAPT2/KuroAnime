package com.kuroanime.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuroanime.data.model.VideoSource
import com.kuroanime.ui.components.KuroTopAppBar
import com.kuroanime.player.enterImmersive
import com.kuroanime.player.exitImmersive
import com.kuroanime.player.play
import androidx.media3.exoplayer.ExoPlayer
import com.kuroanime.player.rememberExoPlayer
import kotlinx.coroutines.delay
import org.json.JSONArray

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
fun WebViewPlayerScreen(url: String, onBack: () -> Unit) {
    val player = rememberExoPlayer()
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var canExtract by remember { mutableStateOf(false) }
    var extractionDone by remember { mutableStateOf(false) }
    var showWebViewFallback by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val fallbackWebViewRef = remember { mutableStateOf<WebView?>(null) }

    val handler = remember { Handler(Looper.getMainLooper()) }

    val referer = remember { Uri.parse(url).let { "${it.scheme}://${it.host}/" } }

    val onVideoFound: (String) -> Unit = { foundUrl ->
        handler.post {
            if (videoUrl == null) {
                Log.d("WebViewPlayer", "Found video URL: $foundUrl")
                videoUrl = foundUrl
                val headers = mutableMapOf(
                    "Referer" to referer,
                    "User-Agent" to "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"
                )
                val cookies = CookieManager.getInstance().getCookie(foundUrl)
                if (!cookies.isNullOrEmpty()) headers["Cookie"] = cookies
                player.play(VideoSource(url = foundUrl, headers = headers))
                extractionDone = true
                isLoading = false
            }
        }
    }

    LaunchedEffect(canExtract) {
        if (!canExtract || extractionDone) return@LaunchedEffect
        delay(5000)
        val wv = webViewRef.value ?: return@LaunchedEffect
        val injectJs = """
            (function() {
                var urls = {};
                function add(u) {
                    if (u && u.indexOf('blob:') !== 0 && /https?:\/\//i.test(u)) urls[u] = true;
                }
                document.querySelectorAll('video, audio').forEach(function(el) {
                    add(el.src);
                    add(el.currentSrc);
                    el.querySelectorAll('source').forEach(function(s) { add(s.src); });
                });
                document.querySelectorAll('[src],[data-src],[data-url],[data-video],[data-file]').forEach(function(el) {
                    ['src','data-src','data-url','data-video','data-file'].forEach(function(a) {
                        var v = el.getAttribute(a);
                        if (v && /\.(mp4|m3u8|ts|webm)(\?|#|$)/i.test(v)) add(v);
                    });
                });
                var text = document.body ? document.body.innerText : '';
                var m;
                var re = /https?:\/\/[^\s<>"']+\.(mp4|m3u8|ts|webm)[^\s<>"']*/gi;
                while ((m = re.exec(text)) !== null) add(m[0]);
                document.querySelectorAll('script').forEach(function(s) {
                    var sc = s.textContent || '';
                    var m2;
                    while ((m2 = re.exec(sc)) !== null) add(m2[0]);
                });
                return Object.keys(urls);
            })();
        """.trimIndent()
        wv.evaluateJavascript(injectJs) { result ->
            if (videoUrl == null) {
                try {
                    val arr = JSONArray(result)
                    val urls = (0 until arr.length()).map { arr.getString(it) }
                    if (urls.isNotEmpty()) {
                        onVideoFound(urls.first())
                    } else {
                        Log.d("WebViewPlayer", "JS extract found nothing")
                    }
                } catch (e: Exception) {
                    Log.e("WebViewPlayer", "JS extract error: ${e.message}")
                }
            }
            extractionDone = true
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        delay(12000)
        if (videoUrl == null) {
            Log.d("WebViewPlayer", "Timeout: showing WebView fallback")
            showWebViewFallback = true
            isLoading = false
        }
    }

    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity

    LaunchedEffect(videoUrl) {
        if (videoUrl != null) {
            delay(300)
            activity?.enterImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(showControls, videoUrl) {
        if (showControls && videoUrl != null) {
            delay(3000)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            activity?.exitImmersive()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            webViewRef.value?.let {
                (it.parent as? ViewGroup)?.removeView(it)
                it.removeAllViews()
                it.destroy()
            }
            fallbackWebViewRef.value?.let {
                (it.parent as? ViewGroup)?.removeView(it)
                it.removeAllViews()
                it.destroy()
            }
        }
    }

    val isPlaying = videoUrl != null

    Scaffold(
        topBar = {
            if (!isPlaying && showWebViewFallback) {
                KuroTopAppBar(
                    title = {
                        Text(pageTitle.ifEmpty { Uri.parse(url).host ?: "Reproductor" })
                    },
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
            if (videoUrl != null) {
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
                                videoUrl = null
                                player.stop()
                                showControls = true
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver al navegador",
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
                                    videoUrl = null
                                    player.stop()
                                    showControls = true
                                }
                            ) {
                                Text("Volver al navegador", color = Color.White)
                            }
                        }
                    }
                }
            } else if (showWebViewFallback) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    factory = { ctx ->
                        @SuppressLint("SetJavaScriptEnabled")
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.userAgentString =
                                "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageTitle = view?.title ?: ""
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (videoUrl != null) return null
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (reqUrl.startsWith("blob:") || reqUrl.startsWith("data:")) return null
                                    if (reqUrl.contains(".js") || reqUrl.contains(".css") ||
                                        reqUrl.contains(".png") || reqUrl.contains(".jpg") ||
                                        reqUrl.contains(".svg") || reqUrl.contains(".ico") ||
                                        reqUrl.contains(".woff") || reqUrl.contains(".ttf") ||
                                        reqUrl.contains(".webp")) return null
                                    val path = Uri.parse(reqUrl).path ?: ""
                                    val isVideo = path.contains(".m3u8") ||
                                        path.contains(".mp4") ||
                                        path.contains(".webm") ||
                                        reqUrl.contains("videoplayback")
                                    if (isVideo) {
                                        val finalUrl = reqUrl
                                        handler.post { onVideoFound(finalUrl) }
                                    }
                                    return null
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    pageTitle = title ?: ""
                                }
                            }
                            fallbackWebViewRef.value = this
                            loadUrl(url)
                        }
                    }
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Resolviendo...",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                AndroidView(
                    modifier = Modifier.size(0.dp),
                    factory = { ctx ->
                        @SuppressLint("SetJavaScriptEnabled")
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString =
                                "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageTitle = view?.title ?: ""
                                    if (!extractionDone && videoUrl == null) canExtract = true
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (videoUrl != null) return null
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (reqUrl.startsWith("blob:") || reqUrl.startsWith("data:")) return null
                                    if (reqUrl.contains(".js") || reqUrl.contains(".css") ||
                                        reqUrl.contains(".png") || reqUrl.contains(".jpg") ||
                                        reqUrl.contains(".svg") || reqUrl.contains(".ico") ||
                                        reqUrl.contains(".woff") || reqUrl.contains(".ttf") ||
                                        reqUrl.contains(".webp")) return null
                                    val path = Uri.parse(reqUrl).path ?: ""
                                    val isVideo = path.contains(".m3u8") ||
                                        path.contains(".mp4") ||
                                        path.contains(".webm") ||
                                        reqUrl.contains("videoplayback")
                                    if (isVideo) {
                                        val finalUrl = reqUrl
                                        handler.post { onVideoFound(finalUrl) }
                                    }
                                    return null
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    pageTitle = title ?: ""
                                }
                            }
                            webViewRef.value = this
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}
