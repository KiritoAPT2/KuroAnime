# Baseline Profile for KuroAnime
# Pre-compile critical code paths for faster startup

# Main Activity and theme
-keepclassmembers class com.kuroanime.MainActivity { *; }
-keepclassmembers class com.kuroanime.ui.theme.KuroAnimeThemeKt { *; }
-keepclassmembers class com.kuroanime.ui.theme.Color { *; }

# Navigation
-keepclassmembers class com.kuroanime.ui.screens.MainScreenKt { *; }
-keepclassmembers class com.kuroanime.ui.screens.Screen { *; }

# Home screen
-keepclassmembers class com.kuroanime.ui.screens.home.HomeScreenKt { *; }
-keepclassmembers class com.kuroanime.ui.screens.home.ContinueWatchingRowKt { *; }
-keepclassmembers class com.kuroanime.ui.screens.home.LatestEpisodeRowKt { *; }

# Search
-keepclassmembers class com.kuroanime.ui.screens.search.SearchScreenKt { *; }

# Player
-keepclassmembers class com.kuroanime.ui.screens.player.PlayerScreenKt { *; }

# Anime info
-keepclassmembers class com.kuroanime.ui.screens.animeinfo.AnimeInfoScreenKt { *; }

# Design system
-keepclassmembers class com.kuroanime.ui.design.DimensKt { *; }

# Data layer
-keepclassmembers class com.kuroanime.data.SettingsManager { *; }
-keepclassmembers class com.kuroanime.data.ThemeMode { *; }

# Compose runtime
-keepclassmembers class * implements androidx.compose.runtime.RememberObserver { *; }
-keepclassmembers class * extends androidx.compose.runtime.Composer { *; }
