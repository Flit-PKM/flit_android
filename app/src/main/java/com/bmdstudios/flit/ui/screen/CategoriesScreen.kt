package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight
import com.bmdstudios.flit.ui.viewmodel.CategoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Categories screen composable.
 * Displays a list of categories with options to create, edit, and delete them.
 */
@Composable
fun CategoriesScreen(
    highlightCategoryActions: Boolean = false
) {
    val viewModel: CategoriesViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    categoryName = ""
                    errorMessage = null
                    showCreateDialog = true
                },
                modifier = Modifier
                    .padding(16.dp)
                    .onboardingPulseHighlight(
                        enabled = highlightCategoryActions,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        maxScale = 1.04f
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Category"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (categories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No categories yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap the + button to create your first category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = categories,
                        key = { it.id }
                    ) { category ->
                        CategoryCard(
                            category = category,
                            highlightActions = highlightCategoryActions,
                            onEditClick = {
                                selectedCategory = category
                                categoryName = category.name
                                errorMessage = null
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                selectedCategory = category
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Category Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                categoryName = ""
                errorMessage = null
            },
            title = { Text("Create Category") },
            text = {
                Column {
                    TextField(
                        value = categoryName,
                        onValueChange = {
                            categoryName = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category Name") },
                        singleLine = true,
                        isError = errorMessage != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryName.trim().isEmpty()) {
                            errorMessage = "Category name cannot be empty"
                        } else {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    viewModel.createCategory(categoryName)
                                    showCreateDialog = false
                                    categoryName = ""
                                    errorMessage = null
                                } catch (e: IllegalArgumentException) {
                                    errorMessage = e.message ?: "Failed to create category"
                                    Timber.e(e, "Error creating category")
                                } catch (e: Exception) {
                                    errorMessage = "An error occurred. Please try again."
                                    Timber.e(e, "Error creating category")
                                }
                            }
                        }
                    },
                    enabled = categoryName.trim().isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        categoryName = ""
                        errorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Category Dialog
    if (showEditDialog && selectedCategory != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                selectedCategory = null
                categoryName = ""
                errorMessage = null
            },
            title = { Text("Edit Category") },
            text = {
                Column {
                    TextField(
                        value = categoryName,
                        onValueChange = {
                            categoryName = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category Name") },
                        singleLine = true,
                        isError = errorMessage != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryName.trim().isEmpty()) {
                            errorMessage = "Category name cannot be empty"
                        } else {
                            val category = selectedCategory!!
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    viewModel.updateCategory(category.copy(name = categoryName))
                                    showEditDialog = false
                                    selectedCategory = null
                                    categoryName = ""
                                    errorMessage = null
                                } catch (e: IllegalArgumentException) {
                                    errorMessage = e.message ?: "Failed to update category"
                                    Timber.e(e, "Error updating category")
                                } catch (e: Exception) {
                                    errorMessage = "An error occurred. Please try again."
                                    Timber.e(e, "Error updating category")
                                }
                            }
                        }
                    },
                    enabled = categoryName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        selectedCategory = null
                        categoryName = ""
                        errorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Category Dialog
    if (showDeleteDialog && selectedCategory != null) {
        val category = selectedCategory!!
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedCategory = null
            },
            title = { Text("Delete Category") },
            text = {
                Text("Are you sure you want to delete \"${category.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                viewModel.deleteCategory(category)
                                showDeleteDialog = false
                                selectedCategory = null
                            } catch (e: Exception) {
                                Timber.e(e, "Error deleting category")
                                showDeleteDialog = false
                                selectedCategory = null
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedCategory = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Displays a single category as a card.
 */
@Composable
fun CategoryCard(
    category: CategoryEntity,
    highlightActions: Boolean = false,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Edit button
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(40.dp)
                    .onboardingPulseHighlight(
                        enabled = highlightActions,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        maxScale = 1.1f
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(40.dp)
                    .onboardingPulseHighlight(
                        enabled = highlightActions,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        maxScale = 1.1f
                    )
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
