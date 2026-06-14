package com.kuroanime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.kuroanime.data.SettingsManager
import com.kuroanime.data.ThemeMode
import com.kuroanime.data.local.AppLaunchType
import com.kuroanime.data.local.AppVersionManager
import com.kuroanime.data.model.Episode
import com.kuroanime.data.worker.EpisodeCheckWorker
import com.kuroanime.ui.screens.MainScreen
import com.kuroanime.ui.screens.Screen
import com.kuroanime.ui.screens.onboarding.WelcomeScreen
import com.kuroanime.ui.theme.KuroAnimeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pendingNav = extractPendingNavigation(intent)

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(SettingsManager.getThemeMode()) }
            var dynamicColor by rememberSaveable { mutableStateOf(SettingsManager.getUseDynamicColor()) }
            var launchType by remember { mutableStateOf<AppLaunchType?>(null) }

            LaunchedEffect(Unit) {
                launchType = withContext(Dispatchers.IO) { AppVersionManager.getLaunchType(this@MainActivity) }
            }

            KuroAnimeTheme(
                themeMode = themeMode,
                pureBlack = themeMode == ThemeMode.DARK_OLED,
                dynamicColor = dynamicColor,
            ) {
                var splashFinished by remember { mutableStateOf(false) }

                val showWelcome = launchType == AppLaunchType.FIRST_INSTALL || launchType == AppLaunchType.UPDATED
                if (launchType == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0F)),
                    )
                } else if (!splashFinished) {
                    com.kuroanime.ui.screens.splash.AnimatedSplashScreen(
                        onSplashFinished = { splashFinished = true },
                    )
                } else if (showWelcome) {
                    WelcomeScreen(
                        onFinished = { launchType = AppLaunchType.NORMAL },
                    )
                } else {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingNav) {
                        if (pendingNav != null) {
                            navController.navigate(pendingNav) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }

                    MainScreen(
                        themeMode = themeMode,
                        onThemeModeChanged = { mode ->
                            themeMode = mode
                            SettingsManager.setThemeMode(mode)
                        },
                        dynamicColor = dynamicColor,
                        onDynamicColorChanged = { enabled ->
                            dynamicColor = enabled
                            SettingsManager.setUseDynamicColor(enabled)
                        },
                        navController = navController,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractPendingNavigation(intent: Intent?): String? {
        if (intent?.getStringExtra(EpisodeCheckWorker.EXTRA_NAV) != "player") return null
        val episodeUrl = intent.getStringExtra(EpisodeCheckWorker.EXTRA_EPISODE_URL) ?: return null
        val source = intent.getStringExtra(EpisodeCheckWorker.EXTRA_SOURCE) ?: return null
        return Screen.Player.createRoute(
            episode = Episode(
                number = intent.getIntExtra(EpisodeCheckWorker.EXTRA_EPISODE_NUMBER, 0),
                title = intent.getStringExtra(EpisodeCheckWorker.EXTRA_EPISODE_TITLE) ?: "",
                url = episodeUrl,
            ),
            source = source,
            animeUrl = intent.getStringExtra(EpisodeCheckWorker.EXTRA_ANIME_URL) ?: "",
            animeTitle = intent.getStringExtra(EpisodeCheckWorker.EXTRA_ANIME_TITLE) ?: "",
            animeImage = intent.getStringExtra(EpisodeCheckWorker.EXTRA_ANIME_IMAGE) ?: "",
        )
    }
}
