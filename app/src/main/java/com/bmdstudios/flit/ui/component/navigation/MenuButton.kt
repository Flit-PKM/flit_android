package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.navigation.Screen

/**
 * Menu button with dropdown menu in the top app bar.
 */
@Composable
fun MenuButton(navController: NavHostController) {
    var menuExpanded by remember { mutableStateOf(false) }

    IconButton(onClick = { menuExpanded = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Menu"
        )
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Categories") },
            onClick = {
                menuExpanded = false
                navController.navigate(Screen.Categories.route)
            }
        )
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                menuExpanded = false
                navController.navigate(Screen.Settings.route)
            }
        )
    }
}
