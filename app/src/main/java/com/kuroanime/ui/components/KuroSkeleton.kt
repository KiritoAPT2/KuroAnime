package com.kuroanime.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    val isDark = MaterialTheme.colorScheme.background.run { red < 0.3f && green < 0.3f && blue < 0.3f }
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceBright

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = KuroShape.medium,
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(KuroDimens.cardWidthWide)) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(KuroDimens.cardAspectRatio),
            shape = KuroShape.medium,
        )
        Spacer(Modifier.height(8.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(6.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp),
            shape = KuroShape.small,
        )
    }
}

@Composable
fun SkeletonRow(
    itemCount: Int = 6,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KuroDimens.spacingMd),
        horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
    ) {
        repeat(itemCount) {
            SkeletonCard()
        }
    }
}

@Composable
fun SkeletonBanner(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(KuroDimens.heroHeight),
            shape = RoundedCornerShape(0.dp),
        )
        Spacer(Modifier.height(KuroDimens.spacingMd))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(24.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(KuroDimens.spacingXs))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(18.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(KuroDimens.spacingSm))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .height(36.dp),
            shape = KuroShape.small,
        )
    }
}

@Composable
fun SkeletonSection(
    titleWidth: Dp = 180.dp,
    itemCount: Int = 6,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(KuroDimens.spacingLg))
        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = KuroDimens.spacingMd)
                .width(titleWidth)
                .height(22.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(KuroDimens.spacingSm))
        SkeletonRow(itemCount = itemCount)
    }
}

@Composable
fun HomeSkeletonLoading() {
    val brush = shimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(80.dp))

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(width = 140.dp, height = 20.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(8.dp))
        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(width = 220.dp, height = 300.dp),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(28.dp))

        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(width = 100.dp, height = 20.dp),
            shape = KuroShape.small,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
            }
        }
    }
}
