package com.bmdstudios.flit.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLineClassifierTest {

    @Test
    fun classifyLine_detectsTaskItems_asDistinctFromUnorderedLists() {
        val unchecked = MarkdownLineClassifier.classifyLine("- [ ] todo", inCodeBlock = false)
        val checked = MarkdownLineClassifier.classifyLine("* [x] done", inCodeBlock = false)
        val plainList = MarkdownLineClassifier.classifyLine("- item", inCodeBlock = false)

        assertEquals(MarkdownLineKind.TASK_ITEM, unchecked.kind)
        assertEquals("☐ ", unchecked.visualPrefix)
        assertEquals(MarkdownLineKind.TASK_ITEM, checked.kind)
        assertEquals("☑ ", checked.visualPrefix)
        assertEquals(MarkdownLineKind.UNORDERED_LIST_ITEM, plainList.kind)
        assertEquals("• ", plainList.visualPrefix)
    }

    @Test
    fun classifyLine_detectsOrderedList_andPreservesNumberToken() {
        val dot = MarkdownLineClassifier.classifyLine("1. first", inCodeBlock = false)
        val paren = MarkdownLineClassifier.classifyLine("2) second", inCodeBlock = false)

        assertEquals(MarkdownLineKind.ORDERED_LIST_ITEM, dot.kind)
        assertEquals("1. ", dot.visualPrefix)
        assertEquals(MarkdownLineKind.ORDERED_LIST_ITEM, paren.kind)
        assertEquals("2) ", paren.visualPrefix)
    }

    @Test
    fun classifyLine_handlesHeadingAndBlockquote() {
        val heading = MarkdownLineClassifier.classifyLine("## subtitle", inCodeBlock = false)
        val quote = MarkdownLineClassifier.classifyLine("> quote", inCodeBlock = false)

        assertEquals(MarkdownLineKind.HEADING, heading.kind)
        assertEquals(2, heading.headingLevel)
        assertEquals(MarkdownLineKind.BLOCKQUOTE, quote.kind)
    }

    @Test
    fun classifyLine_handlesCodeFenceTransitions() {
        val fenceStart = MarkdownLineClassifier.classifyLine("```kotlin", inCodeBlock = false)
        val codeLine = MarkdownLineClassifier.classifyLine("- [x] still code", inCodeBlock = true)
        val fenceEnd = MarkdownLineClassifier.classifyLine("```", inCodeBlock = true)

        assertEquals(MarkdownLineKind.CODE_FENCE, fenceStart.kind)
        assertTrue(fenceStart.codeBlockAfterLine)
        assertEquals(MarkdownLineKind.CODE_CONTENT, codeLine.kind)
        assertTrue(codeLine.codeBlockAfterLine)
        assertEquals(MarkdownLineKind.CODE_FENCE, fenceEnd.kind)
    }

    @Test
    fun normalizeForPreview_convertsTaskCheckboxes_only() {
        val input = """
            - [ ] todo
            - [x] done
            - plain item
            1. ordered
        """.trimIndent()

        val normalized = MarkdownLineClassifier.normalizeForPreview(input)
        val lines = normalized.lines()

        assertEquals("☐ todo", lines[0])
        assertEquals("☑ done", lines[1])
        assertEquals("- plain item", lines[2])
        assertEquals("1. ordered", lines[3])
    }
}
