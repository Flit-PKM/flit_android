package com.bmdstudios.flit.ui.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.component.ModelDownloadProgress
import com.bmdstudios.flit.ui.component.NoteCard
import com.bmdstudios.flit.ui.viewmodel.DownloadUiState
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import android.content.Context
import android.content.ContextWrapper

private const val TAG = "HomeScreen"

private const val EXIT_CONFIRM_WINDOW_MS = 2000L

private fun Context.findActivity(): ComponentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Home screen content.
 */
@Composable
fun HomeScreen(
    modelDownloadState: DownloadUiState,
    notesViewModel: NotesViewModel,
    navController: NavHostController,
    noteDetailsEnabled: Boolean = false,
    highlightCoachMarks: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var exitDeadline by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (exitDeadline > now) {
            Timber.tag(TAG).d("Second back within window, finishing activity")
            activity?.finish()
        } else {
            exitDeadline = now + EXIT_CONFIRM_WINDOW_MS
            coroutineScope.launch {
                launch {
                    snackbarHostState.showSnackbar(
                        message = "Press back again to exit",
                        duration = SnackbarDuration.Indefinite
                    )
                }
                delay(EXIT_CONFIRM_WINDOW_MS)
                snackbarHostState.currentSnackbarData?.dismiss()
                exitDeadline = 0L
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                noteDetailsEnabled = noteDetailsEnabled,
                highlightCoachMarks = highlightCoachMarks,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
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
    noteDetailsEnabled: Boolean = false,
    highlightCoachMarks: Boolean = false,
    modifier: Modifier = Modifier
) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsStateWithLifecycle()

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
            items(
                items = notes,
                key = { it.id }
            ) { note ->
                NoteCard(
                    note = note,
                    navController = navController,
                    notesViewModel = notesViewModel,
                    isAppending = appendingNoteId == note.id,
                    showDetails = noteDetailsEnabled,
                    highlightActions = highlightCoachMarks
                )
            }
        }
    }
}
