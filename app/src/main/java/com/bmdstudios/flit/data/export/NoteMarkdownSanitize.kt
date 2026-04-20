package com.bmdstudios.flit.data.export

private val ILLEGAL_FILENAME_CHARS = Regex("""[/\\:*?"<>|]""")
private const val MAX_TITLE_FILENAME_LEN = 80

/**
 * Produces a stable, filesystem-safe slug from a note title for export filenames and wikilinks.
 * Trims, collapses whitespace, removes illegal path characters, truncates length.
 */
fun sanitizeNoteTitleForFilename(title: String): String {
    val collapsed = title.trim().replace(Regex("\\s+"), " ")
    val stripped = ILLEGAL_FILENAME_CHARS.replace(collapsed, "_").trim('.', ' ')
    val base = if (stripped.isEmpty()) "note" else stripped
    return if (base.length <= MAX_TITLE_FILENAME_LEN) base else base.take(MAX_TITLE_FILENAME_LEN).trimEnd('.', ' ')
}

/**
 * Basename without `.md` used as the wikilink key: `{sanitize(title)}_{noteId}`.
 */
fun noteMarkdownLinkKey(title: String, noteId: Long): String =
    "${sanitizeNoteTitleForFilename(title)}_$noteId"
