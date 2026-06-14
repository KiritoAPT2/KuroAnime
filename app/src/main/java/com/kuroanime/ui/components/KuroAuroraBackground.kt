package com.kuroanime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max


private val StaticBloodDark = Color(0xFF8A0010)
private val StaticBloodNeon = Color(0xFFFF0028)

@Composable
fun AuroraBackground(
    isVisible: Boolean,
    dynamicColor: Boolean = false,
    color: Color = Color.Unspecified,
    scrollOffsetProvider: () -> Float,
    maxScrollOffset: Float = 1000f,
    content: @Composable () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDarkTheme = bgColor.red < 0.3f && bgColor.green < 0.3f && bgColor.blue < 0.3f
    val fadeSurface = MaterialTheme.colorScheme.surface
    val fadeContainer = MaterialTheme.colorScheme.surfaceContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    var offset1 by remember { mutableFloatStateOf(0f) }
    var offset2 by remember { mutableFloatStateOf(0f) }
    var offset3 by remember { mutableFloatStateOf(-50f) }

    val duration1 = 10000L
    val duration2 = 13000L
    val duration3 = 15000L

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - startTime

                val progress1 = (elapsed % (duration1 * 2)).toFloat() / duration1
                val raw1 = if (progress1 <= 1f) progress1 else 2f - progress1
                offset1 = raw1 * 100f

                val progress2 = (elapsed % (duration2 * 2)).toFloat() / duration2
                val raw2 = if (progress2 <= 1f) progress2 else 2f - progress2
                offset2 = raw2 * -80f

                val progress3 = (elapsed % (duration3 * 2)).toFloat() / duration3
                val raw3 = if (progress3 <= 1f) progress3 else 2f - progress3
                offset3 = -50f + (raw3 * 100f)

                delay(66)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val bloodRed: Color
                val darkBlood: Color
                val neonGlow: Color
                val lightPrimary: Color
                val lightSecondary: Color
                val lightGlow: Color
                if (dynamicColor) {
                    val p = primaryColor
                    bloodRed = if (isDarkTheme) {
                        Color(p.red.coerceAtMost(0.5f), 0f, 0f)
                    } else {
                        p.copy(alpha = 0.15f)
                    }
                    darkBlood = if (isDarkTheme) {
                        Color(p.red.coerceAtMost(0.2f), 0f, 0f, alpha = p.alpha.coerceAtLeast(0.8f))
                    } else {
                        p.copy(alpha = 0.08f)
                    }
                    neonGlow = p
                    lightPrimary = p.copy(alpha = 0.12f)
                    lightSecondary = p.copy(alpha = 0.08f)
                    lightGlow = p.copy(alpha = 0.15f)
                } else {
                    bloodRed = if (isDarkTheme) StaticBloodDark else StaticBloodDark.copy(alpha = 0.15f)
                    darkBlood = if (isDarkTheme) StaticBloodDark.copy(alpha = 0.8f) else StaticBloodDark.copy(alpha = 0.08f)
                    neonGlow = StaticBloodNeon
                    lightPrimary = StaticBloodDark.copy(alpha = 0.12f)
                    lightSecondary = StaticBloodDark.copy(alpha = 0.08f)
                    lightGlow = StaticBloodNeon.copy(alpha = 0.15f)
                }

                val baseRadius = size.width * 0.8f
                val a = if (isDarkTheme) 0.35f else 0.5f

                val topBrush = Brush.radialGradient(
                    colors = run {
                        val p1 = if (isDarkTheme) bloodRed else lightPrimary
                        val s1 = if (isDarkTheme) neonGlow else lightGlow
                        listOf(p1.copy(alpha = a), s1.copy(alpha = a * 0.8f), p1.copy(alpha = a * 0.6f), s1.copy(alpha = a * 0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius * 1.8f,
                )

                val leftBrush = Brush.radialGradient(
                    colors = run {
                        val p1 = if (isDarkTheme) bloodRed else lightPrimary
                        val s1 = if (isDarkTheme) darkBlood else lightSecondary
                        listOf(p1.copy(alpha = a), s1.copy(alpha = a * 0.8f), p1.copy(alpha = a * 0.6f), p1.copy(alpha = a * 0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius,
                )

                val rightBrush = Brush.radialGradient(
                    colors = run {
                        val p1 = if (isDarkTheme) neonGlow else lightGlow
                        val s1 = if (isDarkTheme) bloodRed else lightPrimary
                        listOf(p1.copy(alpha = a), s1.copy(alpha = a * 0.8f), p1.copy(alpha = a * 0.6f), s1.copy(alpha = a * 0.4f), Color.Transparent)
                    },
                    center = Offset.Zero,
                    radius = baseRadius * 0.9f,
                )

                onDrawBehind {
                    val scrollOffset = scrollOffsetProvider()
                    val opacity = max(0f, 1f - (scrollOffset / maxScrollOffset))
                    val translationY = -(scrollOffset * 0.5f)

                    if (isVisible && opacity > 0) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                            dx = size.width * 0.5f + offset3,
                            dy = -size.height * 0.25f + offset2 + translationY,
                        )
                        drawCircle(
                            brush = topBrush,
                            radius = baseRadius * 1.8f,
                            center = Offset.Zero,
                            alpha = opacity,
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver,
                        )
                        drawContext.canvas.restore()

                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                            dx = -size.width * 0.2f + offset1,
                            dy = -size.height * 0.10f + offset2 + translationY,
                        )
                        drawCircle(
                            brush = leftBrush,
                            radius = baseRadius,
                            center = Offset.Zero,
                            alpha = opacity,
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver,
                        )
                        drawContext.canvas.restore()

                        drawContext.canvas.save()
                        drawContext.canvas.translate(
                            dx = size.width * 1.1f - offset2,
                            dy = -size.height * 0.20f + offset1 + translationY,
                        )
                        drawCircle(
                            brush = rightBrush,
                            radius = baseRadius * 0.9f,
                            center = Offset.Zero,
                            alpha = opacity,
                            blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.SrcOver,
                        )
                        drawContext.canvas.restore()

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    fadeContainer.copy(alpha = 0.15f),
                                    fadeContainer.copy(alpha = 0.5f),
                                    fadeSurface,
                                ),
                            ),
                            size = size,
                            alpha = 1f,
                            blendMode = BlendMode.SrcOver,
                        )
                    }
                }
            }
    ) {
        content()
    }
}
