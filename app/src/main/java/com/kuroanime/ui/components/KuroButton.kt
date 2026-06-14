package com.kuroanime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape

enum class KuroButtonVariant {
    Primary, Outline, Ghost,
}

@Composable
fun KuroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KuroButtonVariant = KuroButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(100),
        label = "buttonScale",
    )

    val scheme = MaterialTheme.colorScheme

    val bgColor = when {
        !enabled && variant == KuroButtonVariant.Primary -> scheme.primary.copy(alpha = 0.4f)
        !enabled -> Color.Transparent
        variant == KuroButtonVariant.Primary -> scheme.primary
        variant == KuroButtonVariant.Outline -> Color.Transparent
        else -> Color.Transparent
    }

    val borderColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.12f)
        variant == KuroButtonVariant.Outline -> scheme.outline
        else -> Color.Transparent
    }

    val textColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.35f)
        variant == KuroButtonVariant.Primary -> scheme.onPrimary
        variant == KuroButtonVariant.Outline -> scheme.primary
        else -> scheme.onSurface
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(KuroShape.medium)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.background(bgColor, KuroShape.medium)
                } else {
                    Modifier.background(bgColor, KuroShape.medium)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
    ) {
        if (variant == KuroButtonVariant.Outline) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(KuroShape.medium)
                    .background(borderColor.copy(alpha = 0.3f), KuroShape.medium),
            )
        }

        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = textColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(KuroDimens.spacingXs))
            }

            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

@Composable
fun KuroPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = tween(100),
        label = "playButtonScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "Reproducir",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
