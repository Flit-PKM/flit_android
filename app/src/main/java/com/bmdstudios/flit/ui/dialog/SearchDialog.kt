package com.bmdstudios.flit.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import timber.log.Timber

private const val TAG = "SearchDialog"

/**
 * Search dialog for searching notes by text and filtering by category.
 */
@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    notesViewModel: NotesViewModel,
    navController: NavHostController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories by notesViewModel.getAllCategoriesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Timber.tag(TAG).d("Search query: $searchQuery, category: $selectedCategoryId")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Search Notes")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Text search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Category filter dropdown
                OutlinedTextField(
                    value = selectedCategoryId?.let { id ->
                        allCategories.find { it.id == id }?.name ?: "All Categories"
                    } ?: "All Categories",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryDropdownExpanded = true },
                    label = { Text("Category") },
                    trailingIcon = {
                        IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select category"
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            selectedCategoryId = null
                            categoryDropdownExpanded = false
                        }
                    )
                    allCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    navController.navigate(
                        Screen.SearchResults.createRoute(
                            query = searchQuery,
                            categoryId = selectedCategoryId ?: -1L
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
