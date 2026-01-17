package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.component.ModelDownloadProgress
import com.bmdstudios.flit.ui.component.NoteCard
import com.bmdstudios.flit.ui.viewmodel.DownloadUiState
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import timber.log.Timber

private const val TAG = "HomeScreen"

/**
 * Home screen content.
 */
@Composable
fun HomeScreen(
    modelDownloadState: DownloadUiState,
    notesViewModel: NotesViewModel,
    navController: NavHostController
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusManager.clearFocus()
                    }
                )
            }
    ) {
        ModelDownloadProgress(
            uiState = modelDownloadState,
            modifier = Modifier.padding(16.dp)
        )
        NotesList(
            notesViewModel = notesViewModel,
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

/**
 * Displays a list of notes as cards.
 */
@Composable
private fun NotesList(
    notesViewModel: NotesViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()

    if (notes.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Record audio to create your first note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    navController = navController,
                    notesViewModel = notesViewModel
                )
            }
        }
    }
}
