package com.bmdstudios.flit.utils.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Post-processes transcribed text with normalization and punctuation.
 * Handles text normalization, case correction, and punctuation application.
 */
@Singleton
class TextPostProcessor @Inject constructor() {

    /**
     * Normalizes Chinese punctuation marks to their English equivalents.
     * Replaces Chinese full-width punctuation with standard English punctuation.
     * 
     * @param text Text to normalize
     * @return Text with normalized punctuation
     */
    fun normalizePunctuation(text: String): String {
        return text
            .replace('。', '.')  // Chinese full stop → English period
            .replace('，', ',')  // Chinese comma → English comma
            .replace('？', '?')  // Chinese question mark → English question mark
            .replace('！', '!')  // Chinese exclamation mark → English exclamation mark
            .replace('：', ':')  // Chinese colon → English colon
            .replace('；', ';')  // Chinese semicolon → English semicolon
    }

    /**
     * Normalizes text case from uppercase to sentence case.
     * Converts the entire string to lowercase, then capitalizes:
     * - The first letter of the string
     * - Letters that follow sentence-ending punctuation (. ! ?)
     * 
     * @param text Text to normalize
     * @return Text with normalized case
     */
    fun normalizeCase(text: String): String {
        if (text.isBlank()) return text

        val lowercased = text.lowercase()
        val result = StringBuilder(lowercased.length)
        var capitalizeNext = true

        for (char in lowercased) {
            when {
                capitalizeNext && char.isLetter() -> {
                    result.append(char.uppercaseChar())
                    capitalizeNext = false
                }
                char in ".!?" -> {
                    result.append(char)
                    capitalizeNext = true
                }
                else -> {
                    result.append(char)
                }
            }
        }

        return result.toString()
    }

    /**
     * Processes text using the punctuation model with normalization.
     * Applies case normalization, punctuation, and final normalization.
     * 
     * @param rawText Raw transcribed text
     * @param applyPunctuation Function to apply punctuation (if available)
     * @return Processed text with normalization and punctuation
     */
    suspend fun processWithPunctuationModel(
        rawText: String,
        applyPunctuation: suspend (String) -> String?
    ): String = withContext(Dispatchers.IO) {
        val normalizedText = normalizeCase(rawText)
        val punctuatedText = applyPunctuation(normalizedText) ?: normalizedText
        val normalizedPunctuation = normalizePunctuation(punctuatedText)
        normalizeCase(normalizedPunctuation)
    }
}
