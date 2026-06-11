package com.kuroanime

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kuroanime.data.HttpClient
import com.kuroanime.data.SettingsManager
import com.kuroanime.data.ThemeMode
import com.kuroanime.extension.ExtensionManager
import com.kuroanime.ui.navigation.KuroAnimeNavGraph
import com.kuroanime.ui.theme.KuroAnimeTheme

class MainActivity : ComponentActivity() {
    private val activityStart = System.currentTimeMillis()

    override fun onDestroy() {
        super.onDestroy()
        WebViewHost.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PERF", "Activity.onCreate before init: ${System.currentTimeMillis() - activityStart}ms")
        WebViewHost.setActivity(this)
        SettingsManager.init(this)
        HttpClient.init(this)
        ExtensionManager.registerJsExtensions(this)
        ExtensionManager.registerKotlinFallbacks(this)
        Log.d("PERF", "Activity.onCreate after init: ${System.currentTimeMillis() - activityStart}ms")
        setContent {
            var themeMode by remember { mutableStateOf(SettingsManager.getThemeMode()) }
            KuroAnimeTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                KuroAnimeNavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        SettingsManager.setThemeMode(mode)
                    }
                )
            }
        }
    }
}
