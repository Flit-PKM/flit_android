package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.ui.component.PrimaryActionButton
import com.bmdstudios.flit.ui.component.PrimaryActionButtonRow
import com.bmdstudios.flit.ui.dialog.AddRelationshipDialog
import com.bmdstudios.flit.ui.util.displayName
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TAG = "NoteEditScreen"

/**
 * Note edit screen that allows editing a note's title and text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    notesViewModel: NotesViewModel,
    navController: NavHostController
) {
    val note by notesViewModel.noteDao.getNoteByIdFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = null)

    val currentCategories by notesViewModel.getCategoriesForNoteFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val allCategories by notesViewModel.getAllCategoriesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val relationships by notesViewModel.getRelationshipsWithNotesFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var titleText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddRelationshipDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize state from note when it loads
    LaunchedEffect(note) {
        note?.let {
            titleText = it.title
            bodyText = it.text
            Timber.tag(TAG).d("Initialized edit screen for note: ${it.id}")
        }
    }

    // Filter available categories (exclude already assigned ones)
    val availableCategories = allCategories.filter { category ->
        !currentCategories.any { it.id == category.id }
    }

    note?.let { currentNote ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            TextField(
                value = bodyText,
                onValueChange = {
                    bodyText = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                label = { Text("Text") },
                minLines = 10,
                maxLines = Int.MAX_VALUE,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Categories Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current categories as chips
                    if (currentCategories.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategories.forEach { category ->
                                Card(
                                    modifier = Modifier,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        notesViewModel.removeCategoryFromNote(noteId, category.id)
                                                        Timber.tag(TAG).d("Removed category ${category.id} from note")
                                                    } catch (e: Exception) {
                                                        val error = ErrorHandler.handleThrowable(e, "removeCategoryFromNote", TAG)
                                                        errorMessage = error
                                                        Timber.tag(TAG).e(e, "Error removing category from note")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove category",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Category button
                    PrimaryActionButton(
                        onClick = { dropdownExpanded = !dropdownExpanded },
                        enabled = availableCategories.isNotEmpty() || allCategories.isEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (allCategories.isEmpty()) "No Categories Available" else "Add Category")
                    }

                    // Dropdown menu for category selection
                    if (dropdownExpanded && availableCategories.isNotEmpty()) {
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                notesViewModel.addCategoryToNote(noteId, category.id)
                                                dropdownExpanded = false
                                                Timber.tag(TAG).d("Added category ${category.id} to note")
                                            } catch (e: Exception) {
                                                val error = ErrorHandler.handleThrowable(e, "addCategoryToNote", TAG)
                                                errorMessage = error
                                                Timber.tag(TAG).e(e, "Error adding category to note")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Error message display
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Relationships Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Relationships",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current relationships as chips
                    if (relationships.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            relationships.forEach { relationshipDisplay ->
                                Card(
                                    modifier = Modifier,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${relationshipDisplay.relationship.type.displayName()}: ${relationshipDisplay.relatedNote.title}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        notesViewModel.removeRelationship(relationshipDisplay.relationship.id)
                                                        Timber.tag(TAG).d("Removed relationship ${relationshipDisplay.relationship.id}")
                                                    } catch (e: Exception) {
                                                        val error = ErrorHandler.handleThrowable(e, "removeRelationship", TAG)
                                                        errorMessage = error
                                                        Timber.tag(TAG).e(e, "Error removing relationship")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove relationship",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Relationship button
                    PrimaryActionButton(
                        onClick = { showAddRelationshipDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Relationship")
                    }
                }
            }

            // Add Relationship Dialog
            if (showAddRelationshipDialog) {
                AddRelationshipDialog(
                    noteId = noteId,
                    notesViewModel = notesViewModel,
                    onDismiss = { showAddRelationshipDialog = false },
                    onRelationshipAdded = { showAddRelationshipDialog = false }
                )
            }

            PrimaryActionButtonRow {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val updatedNote = currentNote.copy(
                                    title = titleText.trim(),
                                    text = bodyText.trim(),
                                    updated_at = System.currentTimeMillis(),
                                    ver = currentNote.ver + 1
                                )
                                notesViewModel.updateNote(updatedNote)
                                Timber.tag(TAG).d("Note updated successfully: ${updatedNote.id}")
                                notesViewModel.scheduleSyncAfterMutation()
                                withContext(Dispatchers.Main) {
                                    navController.popBackStack()
                                }
                            } catch (e: Exception) {
                                val error = ErrorHandler.handleThrowable(e, "updateNote", TAG)
                                errorMessage = error
                                Timber.tag(TAG).e(e, "Error updating note")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = titleText.trim().isNotEmpty() && bodyText.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
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
