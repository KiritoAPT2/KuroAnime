package com.kuroanime.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kuroanime.R

val PastelWhite = Color(0xFFF8F4EF)
val KuroRed = Color(0xFFEF5350)
val WineRed = Color(0xFF722F37)

@Composable
fun KuroAnimeLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.kurotopbar_logo),
        contentDescription = "Logo KuroAnime",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}
