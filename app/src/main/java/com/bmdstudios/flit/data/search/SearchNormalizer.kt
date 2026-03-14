package com.bmdstudios.flit.data.search

/**
 * Normalizes text for note search: lowercase and remove stop words.
 * Used when building notesearch content and when tokenizing the search query.
 */
object SearchNormalizer {

    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "in", "is", "it", "of", "on", "or", "the", "then", "to", "with",
        "yes", "no", "not", "but", "if", "so", "that", "this", "was", "were"
    )

    private val WORD_REGEX = Regex("\\p{L}+|\\d+")

    /**
     * Returns normalized content: lowercase, split into words, stop words removed,
     * joined with single space. Empty input returns "".
     */
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val words = WORD_REGEX.findAll(input.lowercase()).map { it.value }.filter { it !in STOP_WORDS }
        return words.joinToString(" ")
    }

    /**
     * Returns normalized query words (lowercase, no stop words) for scoring.
     * Empty or blank query returns empty list.
     */
    fun queryWords(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return WORD_REGEX.findAll(query.lowercase()).map { it.value }.filter { it !in STOP_WORDS }.toList()
    }
}
