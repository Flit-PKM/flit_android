package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmdstudios.flit.ui.component.NoteCard
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import timber.log.Timber

private const val TAG = "SearchResultsScreen"

/**
 * Search results screen that displays filtered notes based on text query and optional category filter.
 */
@Composable
fun SearchResultsScreen(
    query: String,
    categoryId: Long,
    notesViewModel: NotesViewModel,
    navController: NavHostController
) {
    // Decode the query (handle placeholder for empty queries)
    val decodedQuery = Screen.SearchResults.decodeQuery(query)
    val categoryIdOrNull = if (categoryId == -1L) null else categoryId
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsStateWithLifecycle()
    val searchResultsFlow = remember(decodedQuery, categoryIdOrNull) {
        notesViewModel.searchNotesWithCategoryFlow(
            query = decodedQuery,
            categoryId = categoryIdOrNull
        )
    }
    val notes by searchResultsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Timber.tag(TAG).d("Displaying search results: query='$decodedQuery', categoryId=$categoryIdOrNull, count=${notes.size}")

    if (notes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No notes found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    isAppending = appendingNoteId == note.id
                )
            }
        }
    }
}
