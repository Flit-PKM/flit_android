package com.bmdstudios.flit.ui.util

/**
 * Utility for extracting note titles from text content.
 * Extracts the first 5 words from text to use as note title.
 */
object NoteTitleExtractor {
    /**
     * Extracts the first 5 words from text to use as note title.
     * Returns "Untitled Note" if text is blank or empty.
     *
     * @param text The text content to extract title from
     * @return The extracted title (first 5 words) or "Untitled Note"
     */
    fun extractTitle(text: String): String {
        if (text.isBlank()) {
            return "Untitled Note"
        }
        
        val words = text.trim().split(Regex("\\s+"))
        return when {
            words.isEmpty() -> "Untitled Note"
            words.size <= 5 -> words.joinToString(" ")
            else -> words.take(5).joinToString(" ")
        }
    }
}
