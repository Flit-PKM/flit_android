package com.bmdstudios.flit.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation for markdown editing.
 * Keeps original text unchanged, while hiding syntax markers and applying styles.
 */
class MarkdownEditorVisualTransformation(
    private val baseTextStyle: TextStyle,
    private val codeTextColor: Color,
    private val quoteTextColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalToTransformed = IntArray(original.length + 1)
        val transformedToOriginal = ArrayList<Int>(original.length + 1)
        val builder = AnnotatedString.Builder()
        var transformedOffset = 0
        var inCodeBlock = false

        var lineStart = 0
        while (lineStart <= original.lastIndex) {
            val lineEndExclusive = original.indexOf('\n', startIndex = lineStart).let { idx ->
                if (idx == -1) original.length else idx
            }
            val hasTrailingNewline = lineEndExclusive < original.length
            val line = original.substring(lineStart, lineEndExclusive)

            val lineInfo = MarkdownLineClassifier.classifyLine(
                line = line,
                inCodeBlock = inCodeBlock
            )
            val isFenceLine = lineInfo.kind == MarkdownLineKind.CODE_FENCE
            val lineStyle = when {
                isFenceLine -> null
                inCodeBlock -> SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = codeTextColor
                )
                else -> blockStyleFor(lineInfo)
            }

            val transformedLine = when {
                isFenceLine -> ""
                lineInfo.markerRange != null -> "${lineInfo.visualPrefix}${line.removeRange(lineInfo.markerRange)}"
                else -> line
            }
            val hiddenCount = if (isFenceLine) {
                line.length
            } else {
                lineInfo.markerRange?.let { it.last - it.first + 1 } ?: 0
            }

            for (i in 0 until hiddenCount) {
                originalToTransformed[lineStart + i] = transformedOffset
            }

            val visibleOriginalStart = lineStart + hiddenCount
            val prefixLength = if (isFenceLine) 0 else lineInfo.visualPrefix.length
            transformedLine.forEachIndexed { index, c ->
                builder.append(c)
                val originalIndex = if (index < prefixLength) {
                    visibleOriginalStart
                } else {
                    visibleOriginalStart + (index - prefixLength)
                }
                transformedToOriginal.add(originalIndex.coerceAtMost(original.length))
                if (index >= prefixLength && visibleOriginalStart + (index - prefixLength) <= original.lastIndex) {
                    originalToTransformed[visibleOriginalStart + (index - prefixLength)] = transformedOffset
                }
                transformedOffset++
            }

            if (lineStyle != null && transformedLine.isNotEmpty()) {
                val end = builder.length
                builder.addStyle(lineStyle, end - transformedLine.length, end)
            }

            originalToTransformed[lineEndExclusive] = transformedOffset
            if (hasTrailingNewline) {
                builder.append('\n')
                transformedToOriginal.add(lineEndExclusive)
                transformedOffset++
                originalToTransformed[lineEndExclusive + 1] = transformedOffset
            }

            inCodeBlock = lineInfo.codeBlockAfterLine

            lineStart = lineEndExclusive + 1
        }

        while (transformedToOriginal.size < builder.length + 1) {
            transformedToOriginal.add(original.length)
        }
        transformedToOriginal.add(original.length)
        originalToTransformed[original.length] = transformedOffset

        val mapped = builder.toAnnotatedString()
        return TransformedText(
            text = mapped,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return originalToTransformed[offset.coerceIn(0, original.length)]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (transformedToOriginal.isEmpty()) return 0
                    return transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
                }
            }
        )
    }

    private fun blockStyleFor(lineInfo: MarkdownLineClassification): SpanStyle {
        return when (lineInfo.kind) {
            MarkdownLineKind.HEADING -> {
                val level = lineInfo.headingLevel ?: 1
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = when (level) {
                        1 -> baseTextStyle.fontSize * 1.35f
                        2 -> baseTextStyle.fontSize * 1.2f
                        3 -> baseTextStyle.fontSize * 1.1f
                        else -> baseTextStyle.fontSize
                    }
                )
            }
            MarkdownLineKind.BLOCKQUOTE -> SpanStyle(color = quoteTextColor)
            MarkdownLineKind.UNORDERED_LIST_ITEM,
            MarkdownLineKind.ORDERED_LIST_ITEM,
            MarkdownLineKind.TASK_ITEM -> SpanStyle()
            else -> SpanStyle()
        }
    }
}
