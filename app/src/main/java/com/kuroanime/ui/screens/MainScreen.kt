package com.kuroanime.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.zIndex
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kuroanime.data.ThemeMode
import com.kuroanime.ui.screens.animelatino.AnimeLatinoScreen
import com.kuroanime.ui.screens.calendario.CalendarScreen
import com.kuroanime.ui.screens.explore.ExploreScreen
import com.kuroanime.ui.screens.favorites.FavoritesScreen
import com.kuroanime.ui.screens.home.HomeScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.kuroanime.ui.components.KuroTopBar
import com.kuroanime.ui.components.KuroTopBarTitle
import com.kuroanime.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    navController: NavHostController = rememberNavController(),
    dynamicColor: Boolean = false,
    onDynamicColorChanged: (Boolean) -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = remember(currentDestination) {
        Screen.bottomNavItems.any { item ->
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        val activeIcon = screen.iconIdActive
                        val inactiveIcon = screen.iconIdInactive
                        if (activeIcon != null && inactiveIcon != null) {
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.15f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
                                label = "icon_scale",
                            )
                            val indicatorWidth by animateDpAsState(
                                targetValue = if (selected) 24.dp else 0.dp,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                                label = "indicator_width",
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Box(contentAlignment = Alignment.Center) {
                                        Crossfade(
                                            targetState = selected,
                                            animationSpec = tween(200),
                                            label = "icon_crossfade",
                                        ) { isSelected ->
                                            Icon(
                                                painter = painterResource(if (isSelected) activeIcon else inactiveIcon),
                                                contentDescription = screen.label,
                                                modifier = Modifier.size(24.dp * iconScale),
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(screen.label)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    dynamicColor = dynamicColor,
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onContinueWatchingClick = { cw ->
                        navController.navigate(Screen.Player.createRoute(
                            episode = com.kuroanime.data.model.Episode(
                                number = cw.episodeNumber,
                                title = cw.episodeTitle,
                                url = cw.episodeUrl,
                            ),
                            source = cw.source,
                            animeUrl = cw.animeId,
                            animeTitle = cw.animeTitle,
                            animeImage = cw.animeImage ?: "",
                        ))
                    },
                    onLatestEpisodeClick = { ep ->
                        navController.navigate(Screen.Player.createRoute(
                            episode = com.kuroanime.data.model.Episode(
                                number = ep.episode.filter { it.isDigit() }.toIntOrNull() ?: 1,
                                title = ep.episode,
                                url = ep.episodeUrl,
                            ),
                            source = ep.source,
                            animeUrl = ep.animeUrl,
                            animeTitle = ep.title,
                            animeImage = ep.image,
                        ))
                    },
                    onLatestSectionClick = { navController.navigate(Screen.LatestEpisodes.route) },
                    onSectionClick = { sectionId, source ->
                        navController.navigate(Screen.Section.createRoute(sectionId, source))
                    },
                    onPeliculasClick = { navController.navigate(Screen.Peliculas.route) },
                    onOVAsClick = { navController.navigate(Screen.OVAs.route) },
                    onHistoryClick = { navController.navigate(Screen.History.route) },
                    onFavoritesClick = { navController.navigate(Screen.Favorites.route) },
                    onNewsClick = {
                        navController.navigate(Screen.News.route)
                    },
                    onCalendarClick = { navController.navigate(Screen.Calendario.route) },
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onPeliculasClick = { navController.navigate(Screen.Peliculas.route) },
                    onCalendarioClick = { navController.navigate(Screen.Calendario.route) },
                    onGenreClick = { genre ->
                        navController.navigate(Screen.Genre.createRoute(genre.lowercase(), "AnimeFLV"))
                    },
                )
            }

            composable(Screen.AnimeLatino.route) {
                AnimeLatinoScreen(
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onAnimeClick = { fav ->
                        navController.navigate(Screen.AnimeInfo.createRoute(fav.animeId, fav.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Calendario.route) {
                CalendarScreen(
                    onAnimeClick = { anime ->
                        if (anime.url.isNotBlank()) {
                            navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.AnimeInfo.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) { backStackEntry ->
                val url = URLDecoder.decode(
                    backStackEntry.arguments?.getString("url") ?: "",
                    StandardCharsets.UTF_8.toString(),
                )
                val source = backStackEntry.arguments?.getString("source") ?: ""
                com.kuroanime.ui.screens.animeinfo.AnimeInfoScreen(
                    animeUrl = url,
                    source = source,
                    onEpisodeClick = { episode, animeTitle, animeImage ->
                        navController.navigate(Screen.Player.createRoute(episode, source, url, animeTitle, animeImage))
                    },
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(Screen.Genre.createRoute(genre, source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                    navArgument("episodeNumber") { type = NavType.IntType },
                    navArgument("episodeTitle") { type = NavType.StringType },
                    navArgument("animeUrl") { type = NavType.StringType },
                    navArgument("animeTitle") { type = NavType.StringType },
                    navArgument("animeImage") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
                val source = backStackEntry.arguments?.getString("source") ?: ""
                val epNum = backStackEntry.arguments?.getInt("episodeNumber") ?: 0
                val epTitle = URLDecoder.decode(backStackEntry.arguments?.getString("episodeTitle") ?: "", StandardCharsets.UTF_8.toString())
                val animeUrl = URLDecoder.decode(backStackEntry.arguments?.getString("animeUrl") ?: "", StandardCharsets.UTF_8.toString())
                val animeTitle = URLDecoder.decode(backStackEntry.arguments?.getString("animeTitle") ?: "", StandardCharsets.UTF_8.toString())
                val animeImage = URLDecoder.decode(backStackEntry.arguments?.getString("animeImage") ?: "", StandardCharsets.UTF_8.toString())
                com.kuroanime.ui.screens.player.PlayerScreen(
                    episode = com.kuroanime.data.model.Episode(number = epNum, title = epTitle, url = url),
                    source = source,
                    animeUrl = animeUrl,
                    animeTitle = animeTitle,
                    animeImage = animeImage,
                    onBack = { navController.popBackStack() },
                    onOpenWebView = { wUrl -> navController.navigate(Screen.WebViewPlayer.createRoute(wUrl)) },
                )
            }

            composable(
                route = Screen.Search.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                com.kuroanime.ui.screens.search.SearchScreen(
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Section.route,
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) { backStackEntry ->
                val sectionId = URLDecoder.decode(backStackEntry.arguments?.getString("sectionId") ?: "", StandardCharsets.UTF_8.toString())
                val sectionSource = URLDecoder.decode(backStackEntry.arguments?.getString("source") ?: "", StandardCharsets.UTF_8.toString())
                com.kuroanime.ui.screens.section.SectionScreen(
                    sectionId = sectionId,
                    source = sectionSource,
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Genre.route,
                arguments = listOf(
                    navArgument("genre") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) { backStackEntry ->
                val genre = URLDecoder.decode(backStackEntry.arguments?.getString("genre") ?: "", StandardCharsets.UTF_8.toString())
                val genreSource = URLDecoder.decode(backStackEntry.arguments?.getString("source") ?: "", StandardCharsets.UTF_8.toString())
                com.kuroanime.ui.screens.genre.GenreScreen(
                    genre = genre,
                    source = genreSource,
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Screen.Settings.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                val settingsScope = androidx.compose.runtime.rememberCoroutineScope()
                com.kuroanime.ui.screens.settings.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAppearance = { navController.navigate("settings/appearance") },
                    onNavigateToStorage = { navController.navigate("settings/storage") },
                    onNavigateToNotifications = { navController.navigate("settings/notifications") },
                    onNavigateToAbout = { navController.navigate("settings/about") },
                    onCheckUpdates = {
                        val activity = navController.context as? android.app.Activity
                        if (activity != null) {
                            com.kuroanime.data.update.UpdateManager.checkForUpdates(activity, settingsScope)
                        }
                    },
                )
            }

            composable(
                "settings/appearance",
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                com.kuroanime.ui.screens.settings.SettingsAppearance(
                    themeMode = themeMode,
                    onThemeModeChanged = onThemeModeChanged,
                    dynamicColor = dynamicColor,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                "settings/notifications",
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                com.kuroanime.ui.screens.notifications.NotificationsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                "settings/storage",
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                com.kuroanime.ui.screens.settings.SettingsStorage(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                "settings/about",
                enterTransition = { slideInHorizontally(animationSpec = tween(250)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(200)) { it / 4 } },
                popEnterTransition = { slideInHorizontally(animationSpec = tween(200)) { -it / 4 } },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { it } },
            ) {
                com.kuroanime.ui.screens.settings.SettingsAbout(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.News.route) {
                com.kuroanime.ui.screens.news.NewsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.History.route) {
                com.kuroanime.ui.screens.history.HistoryScreen(
                    onHistoryClick = { entry ->
                        navController.navigate(Screen.AnimeInfo.createRoute(entry.animeId, entry.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Peliculas.route) {
                com.kuroanime.ui.screens.peliculas.PeliculasScreen(
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.OVAs.route) {
                com.kuroanime.ui.screens.ovas.OVAsScreen(
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.AnimeInfo.createRoute(anime.url, anime.source))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.LatestEpisodes.route) {
                com.kuroanime.ui.screens.latestepisodes.LatestEpisodesScreen(
                    onEpisodeClick = { ep ->
                        navController.navigate(Screen.Player.createRoute(
                            episode = com.kuroanime.data.model.Episode(
                                number = ep.episode.filter { it.isDigit() }.toIntOrNull() ?: 1,
                                title = ep.episode,
                                url = ep.episodeUrl,
                            ),
                            source = ep.source,
                            animeUrl = ep.animeUrl,
                            animeTitle = ep.title,
                            animeImage = ep.image,
                        ))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.WebViewPlayer.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) { backStackEntry ->
                val webViewUrl = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
                com.kuroanime.ui.screens.player.WebViewPlayerScreen(
                    url = webViewUrl,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        val currentBottomScreen = Screen.bottomNavItems.firstOrNull { item ->
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        }
        if (currentBottomScreen != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .zIndex(10f)
            ) {
                KuroTopBar(
                    showLogo = currentBottomScreen is Screen.Home,
                    title = {
                        when (currentBottomScreen) {
                            is Screen.Home -> KuroTopBarTitle("")
                            is Screen.Explore -> KuroTopBarTitle(
                                "Explorar",
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.explore_filled),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )
                            is Screen.AnimeLatino -> KuroTopBarTitle(
                                "Anime Latino",
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.mic_filled),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )
                            is Screen.Favorites -> KuroTopBarTitle(
                                "Favoritos",
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.favorite_filled),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )
                            is Screen.Calendario -> KuroTopBarTitle(
                                "Calendario",
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.calendar_filled),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                            )
                            else -> KuroTopBarTitle("")
                        }
                    },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                )
            }
        }
    }
}
}

