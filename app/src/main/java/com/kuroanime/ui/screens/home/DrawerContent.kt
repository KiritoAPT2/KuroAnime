package com.kuroanime.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuroanime.R
import com.kuroanime.ui.components.ParticleBackground

data class DrawerCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isGenre: Boolean = false
)

val drawerCategories = listOf(
    DrawerCategory("inicio", "Inicio", Icons.Default.Home),
    DrawerCategory("emision", "En Emisión", Icons.Default.Whatshot),
    DrawerCategory("isekai", "Isekai", Icons.Default.Shield, isGenre = true),
    DrawerCategory("romance", "Romance", Icons.Default.Favorite, isGenre = true),
    DrawerCategory("accion", "Acción", Icons.Default.FlashOn, isGenre = true),
    DrawerCategory("aventura", "Aventura", Icons.Default.Explore, isGenre = true),
    DrawerCategory("latinos", "Animes Latinos", Icons.Default.Language),
)

@Composable
fun DrawerContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    isDrawerOpen: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ParticleBackground(isVisible = isDrawerOpen)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_kuroanime),
                contentDescription = "KUROANIME",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 12.dp),
                contentScale = ContentScale.Fit
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )

        Spacer(Modifier.height(8.dp))

        drawerCategories.forEach { category ->
            val isSelected = selectedCategory == category.id
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .width(3.dp)
                            .height(28.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                            )
                    )
                }
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.label,
                            modifier = Modifier.padding(start = 4.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    },
                    label = {
                        Text(
                            text = category.label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = if (isSelected) 15.sp else 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    },
                    selected = isSelected,
                    onClick = { onCategorySelected(category.id) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            thickness = 0.5.dp
        )

        Spacer(Modifier.height(4.dp))

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            },
            label = {
                Text(
                    "Ajustes",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            selected = false,
            onClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(Modifier.height(12.dp))
            }
        }
    }
