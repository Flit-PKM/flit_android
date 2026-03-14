@file:Suppress("FunctionNaming") // Composable entry points use PascalCase per Android convention

package com.bmdstudios.flit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bmdstudios.flit.ui.theme.MaxPrimaryButtonWidth

/**
 * Filled primary action button capped at [MaxPrimaryButtonWidth], centered when the parent is wider.
 * Fills full width on narrow layouts.
 */
@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .widthIn(max = MaxPrimaryButtonWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Text primary action (e.g. Cancel) with the same width cap and centering as [PrimaryActionButton].
 */
@Composable
fun PrimaryActionTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .widthIn(max = MaxPrimaryButtonWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Row of primary actions (e.g. Save + Cancel) capped at [MaxPrimaryButtonWidth], centered when wider.
 * Use [RowScope.weight] on children to split space inside the row.
 */
@Composable
fun PrimaryActionButtonRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val rowModifier = modifier
        .widthIn(max = MaxPrimaryButtonWidth)
        .fillMaxWidth()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = horizontalArrangement,
            content = content
        )
    }
}
