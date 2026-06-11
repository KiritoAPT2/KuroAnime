package com.kuroanime.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kuroanime.data.ThemeMode

private val DarkRedColorScheme = darkColorScheme(
    primary = DarkRedPrimary,
    onPrimary = DarkRedOnPrimary,
    primaryContainer = DarkRedPrimaryContainer,
    onPrimaryContainer = DarkRedOnPrimaryContainer,
    secondary = DarkRedSecondary,
    onSecondary = DarkRedOnSecondary,
    secondaryContainer = DarkRedSecondaryContainer,
    onSecondaryContainer = DarkRedOnSecondaryContainer,
    tertiary = DarkRedTertiary,
    onTertiary = DarkRedOnTertiary,
    tertiaryContainer = DarkRedTertiaryContainer,
    onTertiaryContainer = DarkRedOnTertiaryContainer,
    surface = OledSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = OledBackground,
    onBackground = OnSurfaceDark,
    outline = DarkRedOutline,
    outlineVariant = DarkRedOutlineVariant,
    inverseSurface = Color(0xFFE8E8EC),
    inverseOnSurface = Color(0xFF0A0A0F),
    inversePrimary = DarkRedInversePrimary,
    surfaceTint = DarkRedPrimary,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
)

private val LightRedColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = RedOnPrimary,
    primaryContainer = RedPrimaryContainer,
    onPrimaryContainer = RedOnPrimaryContainer,
    secondary = RedSecondary,
    onSecondary = RedOnSecondary,
    secondaryContainer = RedSecondaryContainer,
    onSecondaryContainer = RedOnSecondaryContainer,
    surface = RedSurface,
    onSurface = RedOnSurface,
    surfaceVariant = RedSurfaceVariant,
    onSurfaceVariant = RedOnSurfaceVariant,
    background = RedBackground,
    onBackground = RedOnBackground,
    outline = RedOutline,
    outlineVariant = RedOutlineVariant,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFCFCFC),
    inversePrimary = Color(0xFFEF9A9A),
    surfaceTint = RedPrimary,
)

@Composable
fun KuroAnimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK_OLED -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkRedColorScheme else LightRedColorScheme

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
