package com.bmdstudios.flit.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.bmdstudios.flit.ui.navigation.Screen
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel

/**
 * Gets the note title for the top bar when on note detail screen.
 */
@Composable
private fun getNoteTitle(noteId: Long?, notesViewModel: NotesViewModel): String {
    return if (noteId != null && noteId != 0L) {
        val note by notesViewModel.noteDao.getNoteByIdFlow(noteId)
            .collectAsStateWithLifecycle(initialValue = null)
        note?.title ?: "Note"
    } else {
        "Note"
    }
}

/**
 * Gets the category name for the top bar when on notes by category screen.
 */
@Composable
private fun getCategoryName(categoryId: Long?, notesViewModel: NotesViewModel): String {
    return if (categoryId != null && categoryId != 0L) {
        val category by notesViewModel.getCategoryByIdFlow(categoryId)
            .collectAsStateWithLifecycle(initialValue = null)
        category?.name ?: "Category"
    } else {
        "Category"
    }
}

/**
 * Gets the search title for the top bar when on search results screen.
 */
@Composable
private fun getSearchTitle(query: String?, categoryId: Long?, notesViewModel: NotesViewModel): String {
    // Decode the query (handle placeholder for empty queries)
    val decodedQuery = query?.let { Screen.SearchResults.decodeQuery(it) } ?: ""
    val hasQuery = decodedQuery.isNotBlank()
    val hasCategory = categoryId != null && categoryId != -1L && categoryId != 0L
    
    return when {
        hasQuery && hasCategory -> {
            val categoryName = getCategoryName(categoryId, notesViewModel)
            "Search: $decodedQuery in $categoryName"
        }
        hasQuery -> "Search: $decodedQuery"
        hasCategory -> {
            val categoryName = getCategoryName(categoryId, notesViewModel)
            "Search in $categoryName"
        }
        else -> "Search Results"
    }
}

/**
 * Determines the top bar title based on the current route.
 */
@Composable
fun TopBarTitle(
    navBackStackEntry: NavBackStackEntry?,
    notesViewModel: NotesViewModel
): String {
    val currentRoute = navBackStackEntry?.destination?.route
    val noteId = navBackStackEntry?.arguments?.getLong("noteId")
    val categoryId = navBackStackEntry?.arguments?.getLong("categoryId")
    val searchQuery = navBackStackEntry?.arguments?.getString("query")
    val searchCategoryId = navBackStackEntry?.arguments?.getLong("categoryId")

    return when {
        currentRoute?.endsWith("/edit") == true -> {
            val noteTitle = getNoteTitle(noteId, notesViewModel)
            if (noteTitle != "Note") "Edit: $noteTitle" else "Edit Note"
        }
        currentRoute?.startsWith("note/") == true -> getNoteTitle(noteId, notesViewModel)
        currentRoute?.startsWith("notes/category/") == true -> getCategoryName(categoryId, notesViewModel)
        currentRoute?.startsWith("search/") == true -> getSearchTitle(searchQuery, searchCategoryId, notesViewModel)
        currentRoute == Screen.Home.route -> "Flit"
        currentRoute == Screen.Settings.route -> "Settings"
        currentRoute == Screen.Categories.route -> "Categories"
        else -> "Flit"
    }
}
