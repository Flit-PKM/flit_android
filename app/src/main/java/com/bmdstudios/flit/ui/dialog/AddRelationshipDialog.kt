package com.bmdstudios.flit.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmdstudios.flit.data.database.model.RelationshipType
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.ui.util.displayName
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TAG = "AddRelationshipDialog"

/**
 * Dialog for adding a relationship between notes.
 * Two-step flow: first select relationship type, then select the related note.
 */
@Composable
fun AddRelationshipDialog(
    noteId: Long,
    notesViewModel: NotesViewModel,
    onDismiss: () -> Unit,
    onRelationshipAdded: () -> Unit
) {
    var selectedType by remember { mutableStateOf<RelationshipType?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showNoteSelection by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get all relationship types
    val relationshipTypes = RelationshipType.values().toList()

    // Get all notes for search
    val allNotes by notesViewModel.noteDao.getAllNotesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Get current relationships to filter out already related notes
    val currentRelationships by notesViewModel.getRelationshipsForNoteFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Filter notes: exclude current note and notes already related with selected type
    val availableNotes = allNotes.filter { note ->
        note.id != noteId && (selectedType == null || !currentRelationships.any { relationship ->
            val relatedNoteId = if (relationship.note_a_id == noteId) {
                relationship.note_b_id
            } else {
                relationship.note_a_id
            }
            relatedNoteId == note.id && relationship.type == selectedType
        })
    }
    val availableNoteIds = remember(availableNotes) { availableNotes.map { it.id }.toSet() }

    // Ranked search when query is non-blank; otherwise show all available notes
    val searchResultsFlow = remember(searchQuery) {
        notesViewModel.searchNotesWithCategoryFlow(searchQuery, categoryId = null)
    }
    val searchResults by searchResultsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val filteredNotes = if (searchQuery.isBlank()) {
        availableNotes
    } else {
        searchResults.filter { it.id in availableNoteIds }
    }

    Timber.tag(TAG).d("Available notes: ${availableNotes.size}, filtered: ${filteredNotes.size}")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (showNoteSelection) "Select Note" else "Select Relationship Type")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showNoteSelection) {
                    // Step 1: Select relationship type
                    OutlinedTextField(
                        value = selectedType?.displayName() ?: "",
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeDropdownExpanded = true },
                        label = { Text("Relationship Type") },
                        trailingIcon = {
                            IconButton(onClick = { typeDropdownExpanded = !typeDropdownExpanded }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select relationship type"
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        relationshipTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                    showNoteSelection = true
                                    Timber.tag(TAG).d("Selected relationship type: $type")
                                }
                            )
                        }
                    }
                } else {
                    // Step 2: Select note
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Notes") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    if (filteredNotes.isEmpty()) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No notes available"
                            } else {
                                "No notes found matching \"$searchQuery\""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(
                                items = filteredNotes,
                                key = { it.id }
                            ) { note ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedType?.let { type ->
                                                Timber.tag(TAG).d("Adding relationship: noteId=$noteId, relatedNoteId=${note.id}, type=$type")
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        notesViewModel.addRelationship(noteId, note.id, type)
                                                        Timber.tag(TAG).d("Relationship added successfully")
                                                        withContext(Dispatchers.Main) {
                                                            onRelationshipAdded()
                                                        }
                                                    } catch (e: Exception) {
                                                        val error = ErrorHandler.handleThrowable(e, "addRelationship", TAG)
                                                        Timber.tag(TAG).e(e, "Error adding relationship: $error")
                                                    }
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (note.text.isNotEmpty()) {
                                            Text(
                                                text = note.text.take(100) + if (note.text.length > 100) "..." else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showNoteSelection) {
                TextButton(
                    onClick = {
                        showNoteSelection = false
                        searchQuery = ""
                    }
                ) {
                    Text("Back")
                }
            } else {
                Button(
                    onClick = {
                        if (selectedType != null) {
                            showNoteSelection = true
                        }
                    },
                    enabled = selectedType != null
                ) {
                    Text("Next")
                }
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
