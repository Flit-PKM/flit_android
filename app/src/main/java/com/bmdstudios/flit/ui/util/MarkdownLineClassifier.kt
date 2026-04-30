package com.bmdstudios.flit.ui.util

private val unorderedListRegex = Regex("^\\s*[-*+]\\s+")
private val orderedListRegex = Regex("^\\s*(\\d+[.)])\\s+")
private val taskListRegex = Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+")
private val blockQuoteRegex = Regex("^\\s*>\\s*")
private val headingRegex = Regex("^\\s{0,3}(#{1,6})\\s+")

enum class MarkdownLineKind {
    HEADING,
    TASK_ITEM,
    UNORDERED_LIST_ITEM,
    ORDERED_LIST_ITEM,
    BLOCKQUOTE,
    CODE_FENCE,
    CODE_CONTENT,
    PLAIN_TEXT
}

data class MarkdownLineClassification(
    val line: String,
    val kind: MarkdownLineKind,
    val markerRange: IntRange?,
    val visualPrefix: String,
    val headingLevel: Int? = null,
    val codeBlockAfterLine: Boolean = false
)

object MarkdownLineClassifier {
    fun classifyLine(
        line: String,
        inCodeBlock: Boolean
    ): MarkdownLineClassification {
        val isFenceLine = line.trim().startsWith("```")
        if (isFenceLine) {
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.CODE_FENCE,
                markerRange = 0..line.lastIndex,
                visualPrefix = "",
                codeBlockAfterLine = !inCodeBlock
            )
        }

        if (inCodeBlock) {
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.CODE_CONTENT,
                markerRange = null,
                visualPrefix = "",
                codeBlockAfterLine = true
            )
        }

        headingRegex.find(line)?.let { match ->
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.HEADING,
                markerRange = match.range,
                visualPrefix = "",
                headingLevel = match.groupValues[1].length,
                codeBlockAfterLine = false
            )
        }

        taskListRegex.find(line)?.let { match ->
            val checked = match.groupValues[1].equals("x", ignoreCase = true)
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.TASK_ITEM,
                markerRange = match.range,
                visualPrefix = if (checked) "☑ " else "☐ ",
                codeBlockAfterLine = false
            )
        }

        unorderedListRegex.find(line)?.let { match ->
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.UNORDERED_LIST_ITEM,
                markerRange = match.range,
                visualPrefix = "• ",
                codeBlockAfterLine = false
            )
        }

        orderedListRegex.find(line)?.let { match ->
            val marker = match.groupValues[1]
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.ORDERED_LIST_ITEM,
                markerRange = match.range,
                visualPrefix = "$marker ",
                codeBlockAfterLine = false
            )
        }

        blockQuoteRegex.find(line)?.let { match ->
            return MarkdownLineClassification(
                line = line,
                kind = MarkdownLineKind.BLOCKQUOTE,
                markerRange = match.range,
                visualPrefix = "> ",
                codeBlockAfterLine = false
            )
        }

        return MarkdownLineClassification(
            line = line,
            kind = MarkdownLineKind.PLAIN_TEXT,
            markerRange = null,
            visualPrefix = "",
            codeBlockAfterLine = false
        )
    }

    fun normalizeForPreview(markdown: String): String {
        val lines = markdown.lines()
        var inCodeBlock = false
        return lines.joinToString("\n") { line ->
            val classified = classifyLine(line, inCodeBlock)
            inCodeBlock = classified.codeBlockAfterLine
            when (classified.kind) {
                MarkdownLineKind.TASK_ITEM -> {
                    classified.visualPrefix + line.removeRange(classified.markerRange!!)
                }
                else -> line
            }
        }
    }
}
