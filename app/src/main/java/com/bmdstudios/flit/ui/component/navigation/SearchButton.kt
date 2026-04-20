package com.bmdstudios.flit.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight

/**
 * Search button to open search dialog.
 */
@Composable
fun SearchButton(
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
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
        onClick = onClick,
        modifier = highlightModifier
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search"
        )
    }
}
