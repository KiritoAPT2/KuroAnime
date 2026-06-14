package com.kuroanime.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.isActive
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroanime.R
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
    onSplashFinished: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "logo_scale",
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(400),
        label = "logo_alpha",
    )

    var loadingVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            loadingVisible = !loadingVisible
            delay(500)
        }
    }
    val loadingAlpha by animateFloatAsState(
        targetValue = if (loadingVisible) 1f else 0.3f,
        animationSpec = tween(400),
        label = "loading_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LaunchedEffect(Unit) {
            showContent = true
            delay(800)
            onSplashFinished()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.kurotopbar_logo),
                contentDescription = "KuroAnime",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(200.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale,
                        alpha = logoAlpha,
                    ),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Cargando...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.alpha(loadingAlpha),
            )
        }
    }
}
