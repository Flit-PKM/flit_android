package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.dialog.DeleteNoteDialog
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.util.displayName
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TAG = "NoteDetailScreen"

/**
 * Note detail screen that displays a single note's title and text.
 */
@Composable
fun NoteDetailScreen(
    noteId: Long,
    notesViewModel: NotesViewModel,
    navController: NavHostController
) {
    val note by notesViewModel.noteDao.getNoteByIdFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = null)
    val categories by notesViewModel.getCategoriesForNoteFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val relationships by notesViewModel.getRelationshipsWithNotesFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsState()

    note?.let { currentNote ->
        val isAppending = appendingNoteId == currentNote.id
        Timber.tag(TAG).d("Displaying note: ${currentNote.id}")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Render markdown content
                RichText(modifier = Modifier.fillMaxWidth()) {
                    Markdown(
                        content = currentNote.text
                    )
                }

                // Display categories underneath the text
                if (categories.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                Card(
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.NotesByCategory.createRoute(category.id))
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = category.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Display relationships underneath categories
                if (relationships.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Relationships",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            relationships.forEach { relationshipDisplay ->
                                Card(
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.NoteDetail.createRoute(relationshipDisplay.relatedNote.id))
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "${relationshipDisplay.relationship.type.displayName()}: ${relationshipDisplay.relatedNote.title}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

            // Action buttons row below relationships
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Append button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(60.dp)
                ) {
                    IconButton(
                        onClick = {
                            notesViewModel.startAppending(currentNote.id)
                            navController.navigate(Screen.Home.route)
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Append",
                            modifier = Modifier.size(30.dp),
                            tint = if (isAppending) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(25.dp))
                
                // Edit button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(60.dp)
                ) {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.NoteEdit.createRoute(currentNote.id))
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(25.dp))
                
                // Delete button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(60.dp)
                ) {
                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog) {
            DeleteNoteDialog(
                note = currentNote,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            notesViewModel.deleteNote(currentNote.id, currentNote.recording)
                        }
                        // Navigate back to home after deletion completes (on main thread)
                        navController.navigate(Screen.Home.route) {
                            // Pop the detail screen from the back stack
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                        }
                    }
                }
            )
        }
    } ?: run {
        // Loading or note not found
        Timber.tag(TAG).w("Note not found or loading: noteId=$noteId")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (noteId != 0L) "Loading..." else "Note not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
