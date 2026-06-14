package com.kuroanime.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuroanime.R
import com.kuroanime.data.StorageManager
import com.kuroanime.ui.components.KuroTopAppBar
import com.kuroanime.ui.components.SettingsCategory
import com.kuroanime.ui.components.SettingsCategoryItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStorage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageCacheSize by remember { mutableStateOf<Long?>(null) }
    var continueSize by remember { mutableStateOf<Long?>(null) }
    var historySize by remember { mutableStateOf<Long?>(null) }
    var notifSize by remember { mutableStateOf<Long?>(null) }
    var anilistSize by remember { mutableStateOf<Long?>(null) }
    var calendarSize by remember { mutableStateOf<Long?>(null) }

    fun refreshSizes() {
        imageCacheSize = StorageManager.getImageCacheSize(context)
        scope.launch {
            continueSize = StorageManager.getJsonFileSize("continue_watching")
            historySize = StorageManager.getJsonFileSize("history")
            notifSize = StorageManager.getJsonFileSize("notification_prefs")
            anilistSize = StorageManager.getJsonFileSize("anilist_cache")
            calendarSize = StorageManager.getJsonFileSize("calendar_cache")
        }
    }

    LaunchedEffect(Unit) { refreshSizes() }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    Scaffold(
        topBar = {
            KuroTopAppBar(
                title = {
                    Column {
                        Text("Almacenamiento", fontWeight = FontWeight.Bold)
                        Text(
                            "Gestionar caché y datos locales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Volver")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        var showClearAllDialog by remember { mutableStateOf(false) }
        var showClearedSnackbar by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            SettingsCategory(
                title = "Uso de almacenamiento",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("Caché de imágenes") },
                        description = { Text(imageCacheSize?.let { formatSize(it) } ?: "Calculando...") },
                        trailingContent = {
                            TextButton(onClick = {
                                StorageManager.clearImageCache(context)
                                imageCacheSize = 0L
                                showClearedSnackbar = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("Continuar viendo") },
                        description = { Text(continueSize?.let { formatSize(it) } ?: "Calculando...") },
                        trailingContent = {
                            TextButton(onClick = {
                                scope.launch {
                                    StorageManager.clearContinueWatching()
                                    continueSize = 0L
                                    showClearedSnackbar = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("Historial") },
                        description = { Text(historySize?.let { formatSize(it) } ?: "Calculando...") },
                        trailingContent = {
                            TextButton(onClick = {
                                scope.launch {
                                    StorageManager.clearHistory()
                                    historySize = 0L
                                    showClearedSnackbar = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("Notificaciones") },
                        description = { Text(notifSize?.let { formatSize(it) } ?: "Calculando...") },
                        trailingContent = {
                            TextButton(onClick = {
                                scope.launch {
                                    StorageManager.clearNotificationPrefs()
                                    notifSize = 0L
                                    showClearedSnackbar = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("AniList (caché)") },
                        description = { Text(anilistSize?.let { formatSize(it) } ?: "Calculando...") },
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Default.Storage,
                        title = { Text("Calendario (caché)") },
                        description = { Text(calendarSize?.let { formatSize(it) } ?: "Calculando...") },
                    ),
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showClearAllDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Borrar todo (excepto favoritos)")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Los favoritos y ajustes no se verán afectados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("¿Borrar todo?") },
                text = { Text("Se eliminarán el historial, continue watching, notificaciones y caché de imágenes. Los favoritos y ajustes se conservarán.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearAllDialog = false
                        scope.launch {
                            StorageManager.clearImageCache(context)
                            StorageManager.clearHistory()
                            StorageManager.clearContinueWatching()
                            StorageManager.clearNotificationPrefs()
                            refreshSizes()
                            showClearedSnackbar = true
                        }
                    }) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showClearedSnackbar) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showClearedSnackbar = false
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Text(
                        text = "Datos eliminados",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}
