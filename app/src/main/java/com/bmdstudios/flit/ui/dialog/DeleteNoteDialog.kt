package com.bmdstudios.flit.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.bmdstudios.flit.data.database.entity.NoteEntity

/**
 * Confirmation dialog for deleting a note.
 */
@Composable
fun DeleteNoteDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Note")
        },
        text = {
            Text("Are you sure you want to delete \"${note.title}\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
