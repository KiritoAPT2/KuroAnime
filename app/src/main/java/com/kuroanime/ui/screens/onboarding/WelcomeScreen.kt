package com.kuroanime.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.init(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    if (state.showOnboarding) {
        val pagerState = rememberPagerState(pageCount = { state.totalSteps })
        val scope = rememberCoroutineScope()

        val bgColor = MaterialTheme.colorScheme.background
        val isDark = remember(bgColor) { bgColor.red < 0.3f && bgColor.green < 0.3f && bgColor.blue < 0.3f }

        val accentColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
            AuroraParticles(modifier = Modifier.fillMaxSize(), accentColor = accentColor, isDark = isDark)

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "page_anim",
                    ) { currentPage ->
                        when (currentPage) {
                            0 -> WelcomePage(
                                isDark = isDark,
                                isFirstInstall = state.isFirstInstall,
                                accentColor = accentColor,
                                onSurfaceColor = onSurfaceColor,
                                onSurfaceVariantColor = onSurfaceVariantColor,
                            )
                            1 -> ChangelogPage(
                                changelogItems = state.changelogItems,
                                accentColor = accentColor,
                                onSurfaceColor = onSurfaceColor,
                                onSurfaceVariantColor = onSurfaceVariantColor,
                            )
                            2 -> PermissionsPage(
                                isDark = isDark,
                                accentColor = accentColor,
                                onSurfaceColor = onSurfaceColor,
                                onSurfaceVariantColor = onSurfaceVariantColor,
                                onRequestPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                            )
                        }
                    }
                }

                PageIndicator(
                    pageCount = state.totalSteps,
                    currentPage = pagerState.currentPage,
                    accentColor = accentColor,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { viewModel.finish(context); onFinished() },
                    ) {
                        Text("Saltar", color = onSurfaceVariantColor.copy(alpha = 0.6f))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < state.totalSteps - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                viewModel.finish(context)
                                onFinished()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isDark) Color.White else Color.White,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp).width(140.dp),
                    ) {
                        Text(
                            if (pagerState.currentPage >= state.totalSteps - 1) "Comenzar" else "Siguiente",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(
    isDark: Boolean,
    isFirstInstall: Boolean,
    accentColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(120.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "K",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "KuroAnime",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Tu reproductor de anime favorito\ncon los mejores servidores latinos",
            fontSize = 15.sp,
            color = onSurfaceVariantColor,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(24.dp))

        if (isFirstInstall) {
            FeatureChip("🎬", "Reproducción optimizada", onSurfaceColor, accentColor)
            Spacer(Modifier.height(10.dp))
            FeatureChip("🖥️", "Múltiples servidores", onSurfaceColor, accentColor)
            Spacer(Modifier.height(10.dp))
            FeatureChip("🎨", "Diseño adaptativo", onSurfaceColor, accentColor)
        } else {
            FeatureChip("🚀", "¡Versión actualizada!", onSurfaceColor, accentColor)
            Spacer(Modifier.height(10.dp))
            FeatureChip("⚡", "Desliza para ver novedades", onSurfaceColor, accentColor)
        }
    }
}

@Composable
private fun FeatureChip(icon: String, text: String, onSurfaceColor: Color, accentColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(260.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = onSurfaceColor.copy(alpha = 0.85f))
    }
}

@Composable
private fun ChangelogPage(
    changelogItems: List<ChangelogItem>,
    accentColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (changelogItems.isEmpty()) "Funciones destacadas" else "Novedades",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Esto es lo que trae esta versión",
            fontSize = 14.sp,
            color = onSurfaceVariantColor,
        )

        Spacer(Modifier.height(32.dp))

        val items = changelogItems.ifEmpty {
            listOf(
                ChangelogItem("🎬", "Reproductor de video mejorado"),
                ChangelogItem("💾", "Caché persistente de episodios"),
                ChangelogItem("🔔", "Notificaciones de nuevos episodios"),
                ChangelogItem("⚡", "Carga más rápida en la pantalla de inicio"),
                ChangelogItem("🎨", "Diseño adaptativo mejorado"),
            )
        }

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.04f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = item.icon, fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = onSurfaceColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    isDark: Boolean,
    accentColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(100.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (isDark) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🔔", fontSize = 44.sp)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Notificaciones",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Activa las notificaciones para recibir\nalertas cuando haya nuevos episodios\nde tus animes favoritos",
            fontSize = 14.sp,
            color = onSurfaceVariantColor,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(32.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    "Permitir notificaciones",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val animProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.3f,
                animationSpec = tween(300),
                label = "dot_alpha",
            )
            Box(
                modifier = Modifier
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = animProgress)),
            )
        }
    }
}

@Composable
private fun AuroraParticles(
    modifier: Modifier = Modifier,
    accentColor: Color,
    isDark: Boolean,
) {
    val particles = remember {
        listOf(
            0.2f to 0.15f, 0.8f to 0.2f, 0.5f to 0.1f,
            0.15f to 0.7f, 0.85f to 0.8f,
            0.3f to 0.5f, 0.7f to 0.6f, 0.6f to 0.3f,
            0.4f to 0.85f, 0.9f to 0.45f,
            0.1f to 0.9f, 0.72f to 0.75f,
            0.5f to 0.12f, 0.25f to 0.3f,
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val particleColor = if (isDark) accentColor.copy(alpha = 0.07f) else accentColor.copy(alpha = 0.04f)
        val glowColor = if (isDark) accentColor.copy(alpha = 0.02f) else accentColor.copy(alpha = 0.01f)

        drawCircle(
            color = glowColor,
            radius = w * 0.4f,
            center = Offset(w * 0.1f, h * 0.15f),
        )
        drawCircle(
            color = glowColor,
            radius = w * 0.3f,
            center = Offset(w * 0.85f, h * 0.7f),
        )
        drawCircle(
            color = glowColor,
            radius = w * 0.25f,
            center = Offset(w * 0.5f, h * 0.9f),
        )

        particles.forEach { (x, y) ->
            drawCircle(
                color = particleColor,
                radius = 1.5f,
                center = Offset(w * x, h * y),
            )
        }
    }
}
