package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bmdstudios.flit.BuildConfig
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight
import com.bmdstudios.flit.ui.navigation.Screen

/**
 * Menu button with dropdown menu in the top app bar.
 */
@Composable
fun MenuButton(
    navController: NavHostController,
    highlighted: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val highlightModifier = if (highlighted) {
        Modifier
            .padding(2.dp)
            .onboardingPulseHighlight(
                enabled = true,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            )
    } else {
        Modifier
    }

    IconButton(
        onClick = { menuExpanded = true },
        modifier = highlightModifier
    ) {
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
        DropdownMenuItem(
            text = { Text("Privacy Policy") },
            onClick = {
                menuExpanded = false
                uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL)
            }
        )
    }
}
