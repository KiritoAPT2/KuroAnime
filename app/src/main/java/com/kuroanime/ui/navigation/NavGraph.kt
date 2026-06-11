package com.kuroanime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kuroanime.data.ThemeMode
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.ui.screens.animeinfo.AnimeInfoScreen
import com.kuroanime.ui.screens.home.HomeScreen
import com.kuroanime.ui.screens.player.PlayerScreen
import com.kuroanime.ui.screens.player.WebViewPlayerScreen
import com.kuroanime.ui.screens.search.SearchScreen
import com.kuroanime.ui.screens.settings.SettingsAbout
import com.kuroanime.ui.screens.settings.SettingsAppearance
import com.kuroanime.ui.screens.settings.SettingsFuentes
import com.kuroanime.ui.screens.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object AnimeInfo : Screen("animeinfo/{url}/{source}") {
        fun createRoute(url: String, source: String): String {
            val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            return "animeinfo/$encoded/$source"
        }
    }
    data object Player : Screen("player/{url}/{source}/{episodeNumber}/{episodeTitle}") {
        fun createRoute(episode: Episode, source: String): String {
            val encoded = URLEncoder.encode(episode.url, StandardCharsets.UTF_8.toString())
            return "player/$encoded/$source/${episode.number}/${URLEncoder.encode(episode.title, StandardCharsets.UTF_8.toString())}"
        }
    }
    data object WebViewPlayer : Screen("webview/{url}") {
        fun createRoute(url: String): String {
            return "webview/${URLEncoder.encode(url, StandardCharsets.UTF_8.toString())}"
        }
    }
}

@Composable
fun KuroAnimeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAnimeClick = { anime ->
                    navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(onAnimeClick = { anime ->
                navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAppearance = { navController.navigate("settings/appearance") },
                onNavigateToFuentes = { navController.navigate("settings/fuentes") },
                onNavigateToAbout = { navController.navigate("settings/about") }
            )
        }
        composable("settings/appearance") {
            SettingsAppearance(
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/fuentes") {
            SettingsFuentes(onBack = { navController.popBackStack() })
        }
        composable("settings/about") {
            SettingsAbout(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.AnimeInfo.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val source = backStackEntry.arguments?.getString("source") ?: ""
            AnimeInfoScreen(
                animeUrl = url,
                source = source,
                onEpisodeClick = { episode ->
                    navController.navigate(Screen.Player.createRoute(episode, source))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType },
                navArgument("episodeNumber") { type = NavType.IntType },
                navArgument("episodeTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val source = backStackEntry.arguments?.getString("source") ?: ""
            val epNum = backStackEntry.arguments?.getInt("episodeNumber") ?: 0
            val epTitle = URLDecoder.decode(backStackEntry.arguments?.getString("episodeTitle") ?: "", StandardCharsets.UTF_8.toString())
            PlayerScreen(
                episode = Episode(number = epNum, title = epTitle, url = url),
                source = source,
                onBack = { navController.popBackStack() },
                onOpenWebView = { url -> navController.navigate(Screen.WebViewPlayer.createRoute(url)) }
            )
        }
        composable(
            route = Screen.WebViewPlayer.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val webViewUrl = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            WebViewPlayerScreen(url = webViewUrl, onBack = { navController.popBackStack() })
        }
    }
}
