package com.bmdstudios.flit.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTitleExtractorTest {

    @Test
    fun extractTitle_returnsUntitledNote_forBlankText() {
        val result = NoteTitleExtractor.extractTitle("   \n\t  ")

        assertEquals("Untitled Note", result)
    }

    @Test
    fun extractTitle_returnsFirstLine_forSingleLineText() {
        val result = NoteTitleExtractor.extractTitle("Buy groceries and meal prep")

        assertEquals("Buy groceries and meal prep", result)
    }

    @Test
    fun extractTitle_returnsFirstNonBlankLine_forMultilineText() {
        val result = NoteTitleExtractor.extractTitle("\n   \nProject kickoff notes\n- agenda\n- attendees")

        assertEquals("Project kickoff notes", result)
    }

    @Test
    fun extractTitle_trimsLeadingAndTrailingWhitespace_fromFirstLine() {
        val result = NoteTitleExtractor.extractTitle("   Daily Standup   \nsecond line")

        assertEquals("Daily Standup", result)
    }

    @Test
    fun extractTitle_stripsMarkdownFormatting_fromFirstLine() {
        val result = NoteTitleExtractor.extractTitle("## [Sprint Plan](https://example.com) **v2**\nbody")

        assertEquals("Sprint Plan v2", result)
    }

    @Test
    fun extractTitleAndBody_removesExtractedTitleLine_fromBody() {
        val result = NoteTitleExtractor.extractTitleAndBody("My Title\nFirst paragraph\nSecond paragraph")

        assertEquals("My Title", result.title)
        assertEquals("First paragraph\nSecond paragraph", result.body)
    }

    @Test
    fun extractTitleAndBody_removesMarkdownTitleLine_andKeepsRemainingMarkdownBody() {
        val result = NoteTitleExtractor.extractTitleAndBody("# **Roadmap**\n- item one\n- item two")

        assertEquals("Roadmap", result.title)
        assertEquals("- item one\n- item two", result.body)
    }
}
