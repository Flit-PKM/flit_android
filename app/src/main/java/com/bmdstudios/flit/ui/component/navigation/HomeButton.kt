package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.navigation.Screen

/**
 * Home button to navigate back to home screen.
 */
@Composable
fun HomeButton(navController: NavHostController) {
    IconButton(
        onClick = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "Home"
        )
    }
}
