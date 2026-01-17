package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Back button to navigate back in the navigation stack.
 */
@Composable
fun BackButton(navController: NavHostController) {
    IconButton(
        onClick = {
            navController.popBackStack()
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}
