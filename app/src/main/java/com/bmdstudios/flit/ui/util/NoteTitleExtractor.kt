package com.bmdstudios.flit.ui.util

/**
 * Utility for extracting note titles from text content.
 * Extracts the first non-blank line from text to use as note title.
 */
object NoteTitleExtractor {
    data class ExtractedNoteContent(
        val title: String,
        val body: String
    )

    /**
     * Extracts the first non-blank line from text to use as note title.
     * Returns "Untitled Note" if text is blank or empty.
     *
     * @param text The text content to extract title from
     * @return The extracted title (first non-blank line) or "Untitled Note"
     */
    fun extractTitle(text: String): String {
        return extractTitleAndBody(text).title
    }

    /**
     * Extracts a plain-text title from the first non-blank line and returns
     * the remaining note body without that extracted title line.
     */
    fun extractTitleAndBody(text: String): ExtractedNoteContent {
        if (text.isBlank()) {
            return ExtractedNoteContent(
                title = "Untitled Note",
                body = ""
            )
        }

        val lines = text.lines()
        val titleLineIndex = lines.indexOfFirst { it.trim().isNotEmpty() }

        if (titleLineIndex < 0) {
            return ExtractedNoteContent(
                title = "Untitled Note",
                body = ""
            )
        }

        val rawTitleLine = lines[titleLineIndex].trim()
        val plainTitle = stripMarkdownFormatting(rawTitleLine).ifBlank { "Untitled Note" }
        val body = lines
            .filterIndexed { index, _ -> index != titleLineIndex }
            .joinToString("\n")
            .trim()

        return ExtractedNoteContent(
            title = plainTitle,
            body = body
        )
    }

    private fun stripMarkdownFormatting(line: String): String {
        return line
            .replace(Regex("^\\s{0,3}#{1,6}\\s*"), "") // headings
            .replace(Regex("^\\s{0,3}[-*+]\\s+"), "") // unordered list
            .replace(Regex("^\\s{0,3}\\d+[.)]\\s+"), "") // ordered list
            .replace(Regex("^\\s{0,3}>\\s*"), "") // blockquote
            .replace(Regex("^\\s{0,3}\\[[ xX]\\]\\s+"), "") // task list
            .replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1") // images
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1") // links
            .replace(Regex("`([^`]*)`"), "$1") // inline code
            .replace(Regex("(\\*\\*|__)(.*?)\\1"), "$2") // bold
            .replace(Regex("(\\*|_)(.*?)\\1"), "$2") // emphasis
            .replace("~~", "") // strikethrough markers
            .trim()
    }
}
