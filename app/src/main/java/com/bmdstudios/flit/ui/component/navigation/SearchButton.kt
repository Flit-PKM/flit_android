package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * Search button to open search dialog.
 */
@Composable
fun SearchButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search"
        )
    }
}
