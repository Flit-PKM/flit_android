package com.bmdstudios.flit.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMarkdownSanitizeTest {

    @Test
    fun sanitize_replacesIllegalPathChars() {
        assertEquals("a_b_c", sanitizeNoteTitleForFilename("a/b:c"))
    }

    @Test
    fun sanitize_collapsesWhitespace() {
        assertEquals("hello world", sanitizeNoteTitleForFilename("  hello   world  "))
    }

    @Test
    fun noteMarkdownLinkKey_usesSanitizedTitleAndId() {
        assertEquals("hello_42", noteMarkdownLinkKey("hello", 42))
    }
}
