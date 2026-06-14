package com.kuroanime.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuroanime.data.model.Anime
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape
import com.kuroanime.ui.theme.AuroraGlowRed
import com.kuroanime.ui.theme.CardShadowBlack
import com.kuroanime.ui.theme.GlassBackground
import com.kuroanime.ui.theme.GlassBackgroundLight
import com.kuroanime.ui.theme.GlassBorderRed

@Composable
fun KuroCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = KuroShape.extraLarge,
    containerColor: Color? = null,
    elevation: Dp = KuroDimens.elevationNone,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.3f && green < 0.3f && blue < 0.3f }
    val glassColor = containerColor ?: if (isDark) GlassBackground else GlassBackgroundLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale",
    )
    val elevationValue by animateDpAsState(
        targetValue = if (isPressed) elevation + 4.dp else elevation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardElevation",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevationValue, shape, ambientColor = CardShadowBlack, spotColor = CardShadowBlack)
            .clip(shape)
            .background(glassColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(colors = listOf(GlassBorderRed, Color.Transparent)),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        content = content,
    )
}

@Composable
fun AnimeCard(
    anime: Anime,
    episodeNumber: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.3f && green < 0.3f && blue < 0.3f }
    val glassBg = if (isDark) GlassBackground else GlassBackgroundLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animeCardScale",
    )
    val elevationValue by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animeCardElevation",
    )

    Column(
        modifier = modifier
            .scale(scale)
            .width(KuroDimens.cardWidthWide)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(KuroDimens.cardAspectRatio)
                .shadow(elevationValue, KuroShape.medium, ambientColor = AuroraGlowRed.copy(alpha = 0.15f), spotColor = AuroraGlowRed.copy(alpha = 0.15f))
                .clip(KuroShape.medium)
                .background(glassBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = listOf(GlassBorderRed, Color.Transparent)),
                    shape = KuroShape.medium,
                ),
        ) {
            if (anime.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(anime.imageUrl)
                        .crossfade(true)
                        .size(240)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )

            if (episodeNumber != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = episodeNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = anime.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
    }
}

@Composable
fun AnimeGridCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.3f && green < 0.3f && blue < 0.3f }
    val glassBg = if (isDark) GlassBackground else GlassBackgroundLight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gridCardScale",
    )
    val elevationValue by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gridCardElevation",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(KuroShape.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(KuroDimens.cardAspectRatio)
                .fillMaxWidth()
                .shadow(elevationValue, KuroShape.medium, ambientColor = AuroraGlowRed.copy(alpha = 0.15f), spotColor = AuroraGlowRed.copy(alpha = 0.15f))
                .clip(KuroShape.medium)
                .background(glassBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = listOf(GlassBorderRed, Color.Transparent)),
                    shape = KuroShape.medium,
                ),
        ) {
            if (anime.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(anime.imageUrl)
                        .crossfade(true)
                        .size(200)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )

            Text(
                text = anime.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}
