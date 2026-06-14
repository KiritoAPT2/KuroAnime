package com.kuroanime.ui.utils

import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import com.kuroanime.ui.screens.Screen

val NavController.canNavigateUp: Boolean
    get() = currentBackStackEntry?.destination?.parent?.route != null

fun NavController.backToMain() {
    while (canNavigateUp && !Screen.bottomNavItems.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}
