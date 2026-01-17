package com.bmdstudios.flit.ui.navigation

/**
 * Navigation routes for the application.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    
    data class NoteDetail(val noteId: Long) : Screen("note/{noteId}") {
        companion object {
            const val ROUTE = "note/{noteId}"
            fun createRoute(noteId: Long) = "note/$noteId"
        }
    }
    
    data class NoteEdit(val noteId: Long) : Screen("note/{noteId}/edit") {
        companion object {
            const val ROUTE = "note/{noteId}/edit"
            fun createRoute(noteId: Long) = "note/$noteId/edit"
        }
    }
    
    data class NotesByCategory(val categoryId: Long) : Screen("notes/category/{categoryId}") {
        companion object {
            const val ROUTE = "notes/category/{categoryId}"
            fun createRoute(categoryId: Long) = "notes/category/$categoryId"
        }
    }
    
    data class SearchResults(val query: String, val categoryId: Long) : Screen("search/{query}/{categoryId}") {
        companion object {
            const val ROUTE = "search/{query}/{categoryId}"
            private const val EMPTY_QUERY_PLACEHOLDER = "-"
            fun createRoute(query: String, categoryId: Long) = 
                "search/${if (query.isBlank()) EMPTY_QUERY_PLACEHOLDER else query}/$categoryId"
            fun decodeQuery(encodedQuery: String): String = 
                if (encodedQuery == EMPTY_QUERY_PLACEHOLDER) "" else encodedQuery
        }
    }
}
