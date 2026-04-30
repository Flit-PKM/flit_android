package com.bmdstudios.flit.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.ui.component.PrimaryActionButton
import com.bmdstudios.flit.ui.dialog.AddRelationshipDialog
import com.bmdstudios.flit.ui.dialog.DeleteNoteDialog
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.onboarding.NoteDetailCoachSection
import com.bmdstudios.flit.ui.onboarding.OnboardingPulseStyle
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight
import com.bmdstudios.flit.ui.util.MarkdownEditorVisualTransformation
import com.bmdstudios.flit.ui.util.displayName
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TAG = "NoteDetailScreen"
private const val AUTO_SAVE_DELAY_MS = 2000L

enum class NoteDetailNavigationAction {
    BACK,
    HOME,
    APPEND,
    DELETE
}

/**
 * Unified note detail/editor screen.
 */
@Composable
fun NoteDetailScreen(
    noteId: Long,
    notesViewModel: NotesViewModel,
    navController: NavHostController,
    titleText: String,
    pendingNavigationAction: NoteDetailNavigationAction? = null,
    onNavigationActionConsumed: () -> Unit = {},
    onboardingSectionHighlight: NoteDetailCoachSection = NoteDetailCoachSection.None
) {
    val scrollState = rememberScrollState()
    val note by notesViewModel.noteDao.getNoteByIdFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = null)
    val currentCategories by notesViewModel.getCategoriesForNoteFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val allCategories by notesViewModel.getAllCategoriesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val relationships by notesViewModel.getRelationshipsWithNotesFlow(noteId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var bodyText by remember(noteId) { mutableStateOf("") }
    var lastSavedBody by remember(noteId) { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddRelationshipDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveIndicatorVisible by remember { mutableStateOf(false) }
    var saveIndicatorEvent by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val bodyTextStyle = MaterialTheme.typography.bodyLarge
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markdownTransformation = remember(bodyTextStyle, primaryColor, onSurfaceVariantColor) {
        MarkdownEditorVisualTransformation(
            baseTextStyle = bodyTextStyle,
            codeTextColor = primaryColor,
            quoteTextColor = onSurfaceVariantColor
        )
    }

    LaunchedEffect(onboardingSectionHighlight, scrollState.maxValue, noteId) {
        if (onboardingSectionHighlight == NoteDetailCoachSection.Categories ||
            onboardingSectionHighlight == NoteDetailCoachSection.Relationships
        ) {
            delay(80)
            if (scrollState.maxValue > 0) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    val availableCategories = allCategories.filter { category ->
        currentCategories.none { it.id == category.id }
    }

    LaunchedEffect(note?.id) {
        note?.let { loaded ->
            bodyText = loaded.text
            lastSavedBody = loaded.text
            Timber.tag(TAG).d("Initialized note editor for note: ${loaded.id}")
        }
    }

    suspend fun flushPendingChangesIfNeeded(showSavedHint: Boolean): Boolean {
        val currentNote = note ?: return true
        val normalizedTitle = titleText.trim()
        val normalizedBody = bodyText.trim()
        val isDirty = normalizedBody != currentNote.text || normalizedTitle != currentNote.title

        if (!isDirty) return true
        if (normalizedTitle.isBlank() || normalizedBody.isBlank()) {
            errorMessage = "Title and text cannot be empty"
            return false
        }

        return try {
            val updatedNote = currentNote.copy(
                title = normalizedTitle,
                text = normalizedBody,
                updated_at = System.currentTimeMillis(),
                ver = currentNote.ver + 1
            )
            withContext(Dispatchers.IO) {
                notesViewModel.updateNote(updatedNote)
                notesViewModel.scheduleSyncAfterMutation()
            }
            lastSavedBody = updatedNote.text
            errorMessage = null
            if (showSavedHint) {
                saveIndicatorEvent++
            }
            true
        } catch (e: Exception) {
            val error = ErrorHandler.handleThrowable(e, "flushPendingChangesIfNeeded", TAG)
            errorMessage = error
            Timber.tag(TAG).e(e, "Error saving note before navigation")
            false
        }
    }

    LaunchedEffect(note?.id, titleText, bodyText) {
        val currentNote = note ?: return@LaunchedEffect
        if (bodyText.trim() == currentNote.text && titleText.trim() == currentNote.title) return@LaunchedEffect
        if (titleText.isBlank() || bodyText.isBlank()) return@LaunchedEffect
        delay(AUTO_SAVE_DELAY_MS)
        flushPendingChangesIfNeeded(showSavedHint = true)
    }

    LaunchedEffect(saveIndicatorEvent) {
        if (saveIndicatorEvent == 0) return@LaunchedEffect
        saveIndicatorVisible = true
        delay(900L)
        saveIndicatorVisible = false
    }

    LaunchedEffect(pendingNavigationAction) {
        when (pendingNavigationAction) {
            NoteDetailNavigationAction.BACK -> {
                if (flushPendingChangesIfNeeded(showSavedHint = false)) {
                    navController.popBackStack()
                }
                onNavigationActionConsumed()
            }
            NoteDetailNavigationAction.HOME -> {
                if (flushPendingChangesIfNeeded(showSavedHint = false)) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
                onNavigationActionConsumed()
            }
            NoteDetailNavigationAction.APPEND -> {
                if (flushPendingChangesIfNeeded(showSavedHint = false)) {
                    notesViewModel.startAppending(noteId)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
                onNavigationActionConsumed()
            }
            NoteDetailNavigationAction.DELETE -> {
                showDeleteDialog = true
                onNavigationActionConsumed()
            }
            null -> Unit
        }
    }

    note?.let { currentNote ->
        BackHandler(enabled = true) {
            coroutineScope.launch {
                if (flushPendingChangesIfNeeded(showSavedHint = false)) {
                    navController.popBackStack()
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            val density = LocalDensity.current
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val hasScrollableContent = scrollState.maxValue.toFloat() > viewportHeightPx
            val showJumpToTop = hasScrollableContent && scrollState.value > 0
            val showJumpToBottom = hasScrollableContent && scrollState.value < scrollState.maxValue

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .onboardingPulseHighlight(
                        enabled = onboardingSectionHighlight == NoteDetailCoachSection.BodyEditor,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = OnboardingPulseStyle.BorderOnly
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicTextField(
                        value = bodyText,
                        onValueChange = {
                            bodyText = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        visualTransformation = markdownTransformation
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingPulseHighlight(
                        enabled = onboardingSectionHighlight == NoteDetailCoachSection.Categories,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = OnboardingPulseStyle.BorderOnly
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

                    if (currentCategories.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategories.forEach { category ->
                                Card(
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.NotesByCategory.createRoute(category.id))
                                    },
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
                                                    } catch (e: Exception) {
                                                        val error = ErrorHandler.handleThrowable(e, "removeCategoryFromNote", TAG)
                                                        errorMessage = error
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
                                            } catch (e: Exception) {
                                                val error = ErrorHandler.handleThrowable(e, "addCategoryToNote", TAG)
                                                errorMessage = error
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingPulseHighlight(
                        enabled = onboardingSectionHighlight == NoteDetailCoachSection.Relationships,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = OnboardingPulseStyle.BorderOnly
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

                    if (relationships.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            relationships.forEach { relationshipDisplay ->
                                Card(
                                    modifier = Modifier.clickable {
                                        navController.navigate(
                                            Screen.NoteDetail.createRoute(relationshipDisplay.relatedNote.id)
                                        )
                                    },
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
                                                    } catch (e: Exception) {
                                                        val error = ErrorHandler.handleThrowable(e, "removeRelationship", TAG)
                                                        errorMessage = error
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

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (showJumpToTop) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                        modifier = Modifier.size(28.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Jump to top",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (showJumpToBottom) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        },
                        modifier = Modifier.size(28.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Jump to bottom",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (saveIndicatorVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(x = 0, y = 28),
                properties = PopupProperties(focusable = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = ". . .Saved!",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showAddRelationshipDialog) {
            AddRelationshipDialog(
                noteId = noteId,
                notesViewModel = notesViewModel,
                onDismiss = { showAddRelationshipDialog = false },
                onRelationshipAdded = { showAddRelationshipDialog = false }
            )
        }

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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                }
            )
        }
    } ?: run {
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
