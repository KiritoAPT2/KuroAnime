package com.kuroanime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kuroanime.R
import com.kuroanime.ui.utils.appBarScrollBehavior

data class NotificationItem(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val time: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuroTopBar(
    title: @Composable () -> Unit,
    showLogo: Boolean = false,
    unreadNotifications: Int = 0,
    notifications: List<NotificationItem> = emptyList(),
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val identityContentColor = MaterialTheme.colorScheme.onSurface
    val scrollBehavior = appBarScrollBehavior()

    var showNotificationMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f),
    ) {
        TopAppBar(
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = identityContentColor,
                        )
                    }
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = if (onBackClick != null) 0.dp else 4.dp),
                ) {
                    if (showLogo) {
                        KuroAnimeLogo()
                    } else {
                        Box(Modifier.weight(1f)) {
                            title()
                        }
                    }
                }
            },
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    IconButton(onClick = onSearchClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Buscar",
                            tint = identityContentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box {
                        IconButton(onClick = { showNotificationMenu = !showNotificationMenu }, modifier = Modifier.size(40.dp)) {
                            Box {
                                Icon(
                                    painter = painterResource(R.drawable.notifications),
                                    contentDescription = "Notificaciones",
                                    tint = identityContentColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (unreadNotifications > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = showNotificationMenu,
                            onDismissRequest = { showNotificationMenu = false },
                        ) {
                            Text(
                                text = "Notificaciones",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            if (notifications.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "No hay notificaciones",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    onClick = { showNotificationMenu = false },
                                )
                            } else {
                                notifications.forEach { notif ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.width(240.dp),
                                            ) {
                                                Icon(
                                                    painter = painterResource(notif.icon),
                                                    contentDescription = null,
                                                    tint = identityContentColor,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        notif.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        notif.subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    notif.time,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = { showNotificationMenu = false },
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = "Ajustes",
                            tint = identityContentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
            ),
        )
    }
}

@Composable
fun KuroTopBarTitle(
    text: String,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
