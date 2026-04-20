package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.model.NoteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMarkdownParserTest {

    @Test
    fun parseExportTimestamp_millis() {
        assertEquals(1700000000000L, parseExportTimestamp("1700000000000"))
    }

    @Test
    fun parseExportTimestamp_isoInstant() {
        val ms = parseExportTimestamp("2024-01-15T12:00:00Z")
        assertEquals(1705320000000L, ms)
    }

    @Test
    fun parseMarkdownNoteFile_full() {
        val md = """
            ---
            title: "My Note"
            categories:
              - Work
              - Ideas
            created: 2024-01-15T12:00:00Z
            updated: 2024-01-16T12:00:00Z
            status: PUBLISHED
            ---

            Body line one.

            ## Relationships
            **Similar To**: [[other_2]]
            **Follows to**: [[parent_3]]
        """.trimIndent()

        val p = parseMarkdownNoteFile("my_1", md)
        assertEquals("my_1", p.linkKey)
        assertEquals("My Note", p.title)
        assertEquals(listOf("Work", "Ideas"), p.categories)
        assertEquals(parseExportTimestamp("2024-01-15T12:00:00Z"), p.createdAtMillis)
        assertEquals(parseExportTimestamp("2024-01-16T12:00:00Z"), p.updatedAtMillis)
        assertEquals(NoteStatus.PUBLISHED, p.status)
        assertTrue(p.body.contains("Body line one"))
        assertEquals(2, p.relationships.size)
        assertEquals("Similar To" to "other_2", p.relationships[0])
        assertEquals("Follows to" to "parent_3", p.relationships[1])
    }

    @Test
    fun validateZipOnlyMarkdownPaths_rejectsNested() {
        assertFalse(
            validateZipOnlyMarkdownPaths(
                listOf("a.md", "folder/b.md")
            )
        )
    }

    @Test
    fun validateZipOnlyMarkdownPaths_acceptsRootMd() {
        assertTrue(
            validateZipOnlyMarkdownPaths(
                listOf("note_1.md", "note_2.md")
            )
        )
    }
}
