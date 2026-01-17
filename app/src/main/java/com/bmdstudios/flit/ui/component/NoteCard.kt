package com.bmdstudios.flit.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.ui.dialog.DeleteNoteDialog
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Displays a single note as a card.
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    navController: NavHostController,
    notesViewModel: NotesViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsState()
    val isAppending = appendingNoteId == note.id

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp) // Space for overlapping buttons
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(Screen.NoteDetail.createRoute(note.id))
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 10.dp), // Extra bottom padding for overlapping buttons
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = note.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Action buttons positioned to overlap bottom border
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = 20.dp), // Half of 40dp button height
            horizontalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            // Append button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                IconButton(
                    onClick = {
                        notesViewModel.startAppending(note.id)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Append",
                        modifier = Modifier.size(20.dp),
                        tint = if (isAppending) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            
            // Edit button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                IconButton(
                    onClick = {
                        navController.navigate(Screen.NoteEdit.createRoute(note.id))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Delete button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                IconButton(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteNoteDialog(
            note = note,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    notesViewModel.deleteNote(note.id, note.recording)
                }
                showDeleteDialog = false
            }
        )
    }
}
