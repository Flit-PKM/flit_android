package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.navigation.Screen

/**
 * Navigation icon in the top app bar.
 * Shows menu button with dropdown on home screen, back button on note detail/edit screens, home button on settings screen.
 */
@Composable
fun NavigationIcon(
    navController: NavHostController,
    currentRoute: String?
) {
    when {
        currentRoute == Screen.Home.route -> {
            MenuButton(navController = navController)
        }
        currentRoute?.startsWith("note/") == true || currentRoute?.startsWith("notes/category/") == true -> {
            BackButton(navController = navController)
        }
        currentRoute == Screen.Settings.route -> {
            MenuButton(navController = navController)
        }
        currentRoute == Screen.Categories.route -> {
            MenuButton(navController = navController)
        }
        else -> {
            // Default to menu button
            MenuButton(navController = navController)
        }
    }
}
