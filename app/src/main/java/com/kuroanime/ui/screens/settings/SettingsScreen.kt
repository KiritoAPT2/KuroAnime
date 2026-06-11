package com.kuroanime.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuroanime.ui.components.SettingsCategory
import com.kuroanime.ui.components.SettingsCategoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToFuentes: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ajustes", fontWeight = FontWeight.Bold)
                        Text(
                            "Personaliza tu experiencia en KuroAnime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            SettingsCategory(
                title = "General",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Filled.Palette,
                        title = { Text("Apariencia") },
                        description = { Text("Tema oscuro OLED, claro o modo sistema") },
                        onClick = onNavigateToAppearance
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Filled.Language,
                        title = { Text("Fuentes") },
                        description = { Text("AnimeFLV, TioAnime, Latanime") },
                        onClick = onNavigateToFuentes
                    ),
                    SettingsCategoryItem(
                        icon = Icons.Filled.Info,
                        title = { Text("Acerca de") },
                        description = { Text("Versión, créditos y licencia") },
                        onClick = onNavigateToAbout
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            SettingsCategory(
                title = "Comunidad",
                items = listOf(
                    SettingsCategoryItem(
                        icon = Icons.Filled.Code,
                        title = { Text("GitHub") },
                        description = { Text("Código fuente y contribuciones") },
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = onNavigateToAbout
                    )
                )
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "KUROANIME v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
