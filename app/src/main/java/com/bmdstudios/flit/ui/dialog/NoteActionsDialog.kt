package com.bmdstudios.flit.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight

enum class NoteActionType {
    PIN,
    APPEND,
    DELETE
}

/**
 * Long-press note actions: Pin, Append (notice), Delete (warning).
 */
@Composable
fun NoteActionsDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onAppend: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    highlightedAction: NoteActionType? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note Options", style = MaterialTheme.typography.titleLarge) },
        text = {
            NoteActionsContent(
                note = note,
                onPin = onPin,
                onAppend = onAppend,
                onDelete = onDelete,
                enabled = enabled,
                highlightedAction = highlightedAction
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoteActionsPanel(
    note: NoteEntity,
    onPin: () -> Unit,
    onAppend: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    highlightedAction: NoteActionType? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Note Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            NoteActionsContent(
                note = note,
                onPin = onPin,
                onAppend = onAppend,
                onDelete = onDelete,
                enabled = enabled,
                highlightedAction = highlightedAction
            )
        }
    }
}

@Composable
private fun NoteActionsContent(
    note: NoteEntity,
    onPin: () -> Unit,
    onAppend: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    highlightedAction: NoteActionType?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onPin,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onboardingPulseHighlight(
                    enabled = highlightedAction == NoteActionType.PIN,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (note.pinned) "Unpin" else "Pin")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onAppend,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onboardingPulseHighlight(
                    enabled = highlightedAction == NoteActionType.APPEND,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Append")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onboardingPulseHighlight(
                    enabled = highlightedAction == NoteActionType.DELETE,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }
        }
    }
}
