package com.bmdstudios.flit.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.ui.dialog.DeleteNoteDialog
import com.bmdstudios.flit.ui.dialog.NoteActionType
import com.bmdstudios.flit.ui.dialog.NoteActionsDialog
import com.bmdstudios.flit.ui.dialog.NoteActionsPanel
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.onboarding.onboardingPulseHighlight
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Displays a single note as a card.
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    isAppending: Boolean,
    showDetails: Boolean = false,
    highlightActions: Boolean = false,
    showOptionsDialog: Boolean = false,
    highlightedDialogAction: NoteActionType? = null,
    /** Home list only: soft centered ribbon under the card top (Material 3 accent). */
    homeListTopAccent: Boolean = false
) {
    var showActionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val ribbonColor = lerp(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        0.5f
    )
    val ribbonShape = RoundedCornerShape(50)
    val ribbonHeight = 8.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isAppending) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = cardShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (highlightActions) {
                            Modifier.border(
                                width = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = cardShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .onboardingPulseHighlight(
                        enabled = highlightActions,
                        shape = cardShape,
                        color = MaterialTheme.colorScheme.primary,
                        maxScale = 1.02f
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            navController.navigate(Screen.NoteDetail.createRoute(note.id))
                        },
                        onLongClick = {
                            showActionsDialog = true
                        }
                    ),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 3.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                    text = note.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                    if (showDetails && note.text.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                        val previewText = note.text.lines().take(3).joinToString("\n").trim()
                        if (previewText.isNotBlank()) {
                            RichText(modifier = Modifier.fillMaxWidth()) {
                                Markdown(content = previewText)
                            }
                        }
                    }
                }
            }

            if (homeListTopAccent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -(ribbonHeight / 2))
                        .fillMaxWidth(0.9f)
                        .height(ribbonHeight)
                        .background(
                            color = ribbonColor,
                            shape = ribbonShape
                        )
                )
            }
        }
    }

    if (showActionsDialog) {
        NoteActionsDialog(
            note = note,
            onDismiss = {
                if (!showOptionsDialog) {
                    showActionsDialog = false
                }
            },
            onPin = {
                coroutineScope.launch(Dispatchers.IO) {
                    notesViewModel.setNotePinned(note.id, !note.pinned)
                }
                if (!showOptionsDialog) {
                    showActionsDialog = false
                }
            },
            onAppend = {
                notesViewModel.startAppending(note.id)
                if (!showOptionsDialog) {
                    showActionsDialog = false
                }
            },
            onDelete = {
                if (!showOptionsDialog) {
                    showActionsDialog = false
                    showDeleteDialog = true
                }
            },
            enabled = true,
            highlightedAction = highlightedDialogAction
        )
    }

    if (showOptionsDialog) {
        NoteActionsPanel(
            note = note,
            onPin = {},
            onAppend = {},
            onDelete = {},
            enabled = false,
            highlightedAction = highlightedDialogAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
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
