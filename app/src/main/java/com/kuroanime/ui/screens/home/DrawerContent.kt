package com.kuroanime.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroanime.R
import com.kuroanime.ui.components.ParticleBackground
import com.kuroanime.ui.design.KuroDimens
import com.kuroanime.ui.design.KuroShape

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DrawerContent(
    onInicioClick: () -> Unit = {},
    onAnimeLatinoClick: () -> Unit = {},
    onPeliculasClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onCalendarioClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAcercaClick: () -> Unit = {},
    isDrawerOpen: Boolean = true
) {
    val mainItems = listOf(
        DrawerItem("Notificaciones", Icons.Default.Notifications, onNotificacionesClick),
    )

    val bottomItems = listOf(
        DrawerItem("Ajustes", Icons.Outlined.Settings, onSettingsClick),
        DrawerItem("Acerca de", Icons.Outlined.Info, onAcercaClick),
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0000),
                        Color(0xFF100000),
                    ),
                ),
            ),
    ) {
        ParticleBackground(isVisible = isDrawerOpen)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_kuroanime),
                    contentDescription = "KUROANIME",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(Modifier.height(KuroDimens.spacingSm))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = KuroDimens.spacingMd),
                color = Color.White.copy(alpha = 0.08f),
                thickness = 0.5.dp,
            )

            Spacer(Modifier.height(KuroDimens.spacingSm))

            mainItems.forEach { item ->
                KuroDrawerItem(
                    icon = item.icon,
                    label = item.label,
                    onClick = item.onClick,
                    primary = true,
                )
            }

            Spacer(Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color.White.copy(alpha = 0.06f),
                thickness = 0.5.dp,
            )

            Spacer(Modifier.height(4.dp))

            bottomItems.forEach { item ->
                KuroDrawerItem(
                    icon = item.icon,
                    label = item.label,
                    onClick = item.onClick,
                    primary = false,
                )
            }

            Spacer(Modifier.height(KuroDimens.spacingMd))
        }
    }
}

@Composable
private fun KuroDrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
) {
    val iconColor = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f)
    val textColor = if (primary) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroDimens.spacingXs, vertical = 1.dp)
            .clip(KuroShape.small)
            .clickable(onClick = onClick)
            .padding(horizontal = KuroDimens.spacingMd, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(if (primary) 22.dp else 20.dp),
        )
        Spacer(Modifier.width(KuroDimens.spacingMd))
        Text(
            text = label,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
            fontSize = if (primary) 15.sp else 14.sp,
            color = textColor,
        )
    }
}
