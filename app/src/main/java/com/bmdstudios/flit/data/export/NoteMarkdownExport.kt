package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.RelationshipType
import java.time.Instant

private fun RelationshipType.exportLabel(): String = when (this) {
    RelationshipType.FOLLOWS_ON -> "Follows On"
    RelationshipType.SIMILAR_TO -> "Similar To"
    RelationshipType.CONTRADICTS -> "Contradicts"
    RelationshipType.REFERENCES -> "References"
    RelationshipType.RELATED_TO -> "Related To"
}

private const val REL_HEADER = "\n## Relationships\n"

private fun yamlScalar(s: String): String {
    val needsQuote = s.any { it == '\n' || it == ':' || it == '#' || it == '"' } ||
        s != s.trim() || s.isEmpty()
    if (!needsQuote) return s
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private fun formatFrontmatter(
    title: String,
    categoryNames: List<String>,
    createdAtMillis: Long,
    updatedAtMillis: Long,
    statusName: String
): String {
    val catBlock = if (categoryNames.isEmpty()) {
        "categories: []\n"
    } else {
        buildString {
            appendLine("categories:")
            categoryNames.forEach { appendLine("  - ${yamlScalar(it)}") }
        }
    }
    return buildString {
        appendLine("---")
        appendLine("title: ${yamlScalar(title)}")
        append(catBlock)
        appendLine("created: ${Instant.ofEpochMilli(createdAtMillis)}")
        appendLine("updated: ${Instant.ofEpochMilli(updatedAtMillis)}")
        appendLine("status: $statusName")
        appendLine("---")
    }
}

private fun relationshipExportLabel(noteId: Long, relationship: RelationshipEntity): String {
    return when (relationship.type) {
        RelationshipType.FOLLOWS_ON ->
            if (noteId == relationship.note_a_id) "Follows to" else "Follows from"
        else -> relationship.type.exportLabel()
    }
}

private fun relationshipLine(
    noteId: Long,
    relationship: RelationshipEntity,
    relatedNote: NoteEntity
): String {
    val label = relationshipExportLabel(noteId, relationship)
    val key = noteMarkdownLinkKey(relatedNote.title, relatedNote.id)
    return "**$label**: [[$key]]"
}

/**
 * Builds full markdown file content for one note (frontmatter + body + optional Relationships section).
 */
fun buildNoteMarkdownFile(
    note: NoteEntity,
    categoryNames: List<String>,
    relationships: List<RelationshipEntity>,
    notesById: Map<Long, NoteEntity>
): String {
    val fm = formatFrontmatter(
        title = note.title,
        categoryNames = categoryNames,
        createdAtMillis = note.created_at,
        updatedAtMillis = note.updated_at,
        statusName = note.workflow_status.name
    )
    val body = note.text
    val relLines = relationships.mapNotNull { rel ->
        val relatedId = if (rel.note_a_id == note.id) rel.note_b_id else rel.note_a_id
        val related = notesById[relatedId] ?: return@mapNotNull null
        relationshipLine(note.id, rel, related)
    }
    return buildString {
        append(fm)
        append(body)
        if (relLines.isNotEmpty()) {
            append(REL_HEADER)
            relLines.forEach { appendLine(it) }
        }
    }
}

/**
 * Zip entry path (root-level): `{linkKey}.md`.
 */
fun noteMarkdownZipEntryName(note: NoteEntity): String =
    "${noteMarkdownLinkKey(note.title, note.id)}.md"
