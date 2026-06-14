package com.kuroanime.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuroanime.BuildConfig
import com.kuroanime.R
import com.kuroanime.data.SettingsManager
import com.kuroanime.ui.components.KuroTopAppBar
import com.kuroanime.ui.components.SettingsCategory
import com.kuroanime.ui.components.SettingsCategoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToStorage: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAbout: () -> Unit,
    onCheckUpdates: () -> Unit = {},
) {
    var autoplayEnabled by remember { mutableStateOf(SettingsManager.getAutoplay()) }

    Scaffold(
        topBar = {
            KuroTopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Volver")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Personaliza tu experiencia en KuroAnime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))

            SettingsCategory(
                title = "Personalización",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Filled.DarkMode,
                        title = { Text("Apariencia") },
                        description = { Text("Tema oscuro OLED, claro o modo sistema", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToAppearance,
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Filled.Notifications,
                        title = { Text("Notificaciones") },
                        description = { Text("Gestionar animes seguidos y alertas", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToNotifications,
                    ),
                ),
            )

            Spacer(Modifier.height(24.dp))

            SettingsCategory(
                title = "Aplicación",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Filled.Storage,
                        title = { Text("Almacenamiento") },
                        description = { Text("Gestionar caché y datos locales", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToStorage,
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Filled.SystemUpdate,
                        title = { Text("Actualizaciones") },
                        description = { Text("Buscar nueva versión disponible", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onCheckUpdates,
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Filled.Info,
                        title = { Text("Acerca de") },
                        description = { Text("Versión, créditos y licencia", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = onNavigateToAbout,
                    ),
                ),
            )

            Spacer(Modifier.height(24.dp))

            SettingsCategory(
                title = "Reproducción",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Filled.PlayCircleOutline,
                        title = { Text("Auto-reproducción") },
                        description = { Text("Pasar al siguiente episodio automáticamente", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Switch(
                                checked = autoplayEnabled,
                                onCheckedChange = {
                                    autoplayEnabled = it
                                    SettingsManager.setAutoplay(it)
                                },
                            )
                        },
                    ),
                ),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "KUROANIME v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
