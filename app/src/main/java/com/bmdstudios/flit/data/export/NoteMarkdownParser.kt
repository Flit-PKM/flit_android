package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.model.NoteStatus
import java.time.Instant
import java.time.format.DateTimeParseException

data class ParsedMarkdownNote(
    val linkKey: String,
    val title: String,
    val categories: List<String>,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val status: NoteStatus?,
    val body: String,
    /** Display label → wikilink target (inside `[[...]]`, no brackets). */
    val relationships: List<Pair<String, String>>
)

private val RELATIONSHIP_LINE = Regex("^\\*\\*(.+?)\\*\\*:\\s*\\[\\[([^]]+)]]\\s*$")
private val REL_SECTION_REGEX = Regex("^## Relationships\\s*$", RegexOption.MULTILINE)

/**
 * Parses `created` / `updated` as epoch millis (digits only) or ISO-8601 instant.
 */
fun parseExportTimestamp(raw: String): Long? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    if (s.all { it.isDigit() }) {
        return s.toLongOrNull()
    }
    return try {
        Instant.parse(s).toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Splits full file text into optional YAML frontmatter (between first two `---` lines) and remainder.
 */
fun splitFrontMatter(text: String): Pair<String?, String> {
    val lines = text.lines()
    if (lines.isEmpty() || lines.first().trim() != "---") {
        return null to text
    }
    val endIndex = (1 until lines.size).firstOrNull { lines[it].trim() == "---" } ?: -1
    if (endIndex < 0) {
        return null to text
    }
    val fm = lines.subList(1, endIndex).joinToString("\n")
    val rest = lines.subList(endIndex + 1, lines.size).joinToString("\n").trimStart('\n')
    return fm to rest
}

private fun unquoteYamlScalar(s: String): String {
    val t = s.trim()
    if (t.length >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
        return t.drop(1).dropLast(1)
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
    return t
}

private fun parseCategoryListLine(line: String): String? {
    val t = line.trim()
    if (!t.startsWith("- ")) return null
    return unquoteYamlScalar(t.removePrefix("- ").trim())
}

private data class FrontmatterResult(
    val title: String,
    val categories: List<String>,
    val createdRaw: String?,
    val updatedRaw: String?,
    val statusRaw: String?
)

private fun parseFrontmatterBlock(fm: String): FrontmatterResult {
    var title = ""
    val categories = mutableListOf<String>()
    var createdRaw: String? = null
    var updatedRaw: String? = null
    var statusRaw: String? = null
    var inCategories = false

    fun finishCategoriesLine(line: String): Boolean {
        val cat = parseCategoryListLine(line) ?: return false
        categories.add(cat)
        return true
    }

    fm.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            inCategories = false
            return@forEach
        }
        if (inCategories && finishCategoriesLine(line)) {
            return@forEach
        }
        val colon = line.indexOf(':')
        if (colon <= 0) return@forEach
        val key = line.substring(0, colon).trim().lowercase()
        val value = line.substring(colon + 1).trim()
        when (key) {
            "categories", "ategories" -> {
                inCategories = true
                if (value == "[]" || value.isEmpty()) {
                    // empty list
                }
            }
            "title" -> {
                inCategories = false
                title = unquoteYamlScalar(value)
            }
            "created" -> {
                inCategories = false
                createdRaw = value
            }
            "updated" -> {
                inCategories = false
                updatedRaw = value
            }
            "status" -> {
                inCategories = false
                statusRaw = value
            }
            else -> {
                inCategories = false
            }
        }
    }
    return FrontmatterResult(title, categories, createdRaw, updatedRaw, statusRaw)
}

private fun splitBodyAndRelationships(rest: String): Pair<String, List<Pair<String, String>>> {
    val m = REL_SECTION_REGEX.find(rest) ?: return rest.trimEnd() to emptyList()
    val body = rest.substring(0, m.range.first).trimEnd()
    val afterHeader = rest.substring(m.range.last + 1).trimStart('\n', '\r')
    val rels = afterHeader.lines().mapNotNull { l ->
        val mline = RELATIONSHIP_LINE.matchEntire(l.trim()) ?: return@mapNotNull null
        mline.groupValues[1].trim() to mline.groupValues[2].trim()
    }
    return body to rels
}

fun parseNoteStatus(name: String): NoteStatus? =
    NoteStatus.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

/**
 * Parses one markdown note file. [linkKey] is the basename without `.md`.
 */
fun parseMarkdownNoteFile(linkKey: String, fullText: String): ParsedMarkdownNote {
    val (fmOpt, afterFm) = splitFrontMatter(fullText)
    val title: String
    val categories: List<String>
    val createdAt: Long?
    val updatedAt: Long?
    val status: NoteStatus?
    if (fmOpt != null) {
        val r = parseFrontmatterBlock(fmOpt)
        title = r.title
        categories = r.categories
        createdAt = r.createdRaw?.let { parseExportTimestamp(it) }
        updatedAt = r.updatedRaw?.let { parseExportTimestamp(it) }
        status = r.statusRaw?.let { parseNoteStatus(it) }
    } else {
        title = ""
        categories = emptyList()
        createdAt = null
        updatedAt = null
        status = null
    }
    val (body, rels) = splitBodyAndRelationships(afterFm)
    return ParsedMarkdownNote(
        linkKey = linkKey,
        title = title.ifBlank { linkKey },
        categories = categories,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        status = status,
        body = body,
        relationships = rels
    )
}

/**
 * True if this zip entry path is a non-directory markdown file at archive root (no subfolders).
 */
fun zipEntryShouldRead(path: String): Boolean {
    val normalized = path.trim().removePrefix("/").removeSuffix("/")
    if (normalized.isEmpty()) return false
    if (normalized.startsWith("__MACOSX/", ignoreCase = true)) return false
    if (normalized.contains('/')) return false
    if (!normalized.endsWith(".md", ignoreCase = true)) return false
    if (normalized.equals(".DS_Store", ignoreCase = true)) return false
    return true
}

/**
 * True if every non-directory file entry is `.md` at archive root only (no subfolders);
 * ignores `__MACOSX/` and `.DS_Store`.
 */
fun validateZipOnlyMarkdownPaths(paths: List<String>): Boolean {
    for (path in paths) {
        val n = path.trim().removePrefix("/")
        if (n.endsWith("/")) continue
        if (n.startsWith("__MACOSX/", ignoreCase = true)) continue
        if (n.equals(".DS_Store", ignoreCase = true)) continue
        if (n.contains('/')) return false
        if (!n.endsWith(".md", ignoreCase = true)) return false
    }
    return true
}
