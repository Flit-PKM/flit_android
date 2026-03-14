package com.bmdstudios.flit.data.search

import com.bmdstudios.flit.data.database.entity.NoteSearchRow

/** Score and ranking constants. */
private const val BOOST_EXACT_PHRASE = 100f
private const val BOOST_ALL_WORDS = 20f
private const val BOOST_PER_WORD = 2f
private const val BONUS_FUZZY = 0.5f
private const val FUZZY_RATIO_THRESHOLD = 0.8f

/**
 * Scores and ranks note search candidates. No title weighting; single content field.
 * Primary: prefix or substring match per query word. Secondary: fuzzy similarity bonus.
 */
object NoteSearchScorer {

    private val WORD_REGEX = Regex("\\p{L}+|\\d+")

    /**
     * Returns candidate note IDs sorted by score descending, then by updated_at descending.
     * [queryWords] should be normalized (e.g. from SearchNormalizer.queryWords).
     */
    fun rank(
        queryWords: List<String>,
        candidates: List<NoteSearchRow>
    ): List<Long> {
        if (queryWords.isEmpty()) {
            return candidates.sortedByDescending { it.updated_at }.map { it.note_id }
        }

        val scored = candidates.map { row ->
            row.note_id to scoreRow(queryWords, row)
        }
        return scored
            .filter { it.second > 0f }
            .sortedWith(
                compareByDescending<Pair<Long, Float>> { it.second }
                    .thenByDescending { (noteId, _) ->
                        candidates.find { it.note_id == noteId }!!.updated_at
                    }
            )
            .map { it.first }
    }

    private fun scoreRow(queryWords: List<String>, row: NoteSearchRow): Float {
        val content = row.content
        if (content.isBlank()) return 0f

        val contentWords = WORD_REGEX.findAll(content).map { it.value }.toList()
        var score = 0f

        // Exact whole phrase in content → highest boost
        val fullQuery = queryWords.joinToString(" ")
        if (fullQuery.length >= 2 && content.contains(fullQuery)) {
            score += BOOST_EXACT_PHRASE
        }

        // Per-word: prefix of any content word or substring in content
        var matchedWords = 0
        for (q in queryWords) {
            if (q.length < 2) continue
            val prefixMatch = contentWords.any { w -> w.startsWith(q) || q.startsWith(w) }
            val substringMatch = content.contains(q)
            if (prefixMatch || substringMatch) {
                score += BOOST_PER_WORD
                matchedWords++
            }
        }

        // All query words present → high boost
        if (matchedWords == queryWords.size && queryWords.isNotEmpty()) {
            score += BOOST_ALL_WORDS
        }

        // Fuzzy: small bonus for words that are similar but not exact
        for (q in queryWords) {
            if (q.length < 2) continue
            val best = contentWords.maxOfOrNull { w -> similarityRatio(q, w) } ?: 0f
            if (best >= FUZZY_RATIO_THRESHOLD && best < 1f) {
                score += BONUS_FUZZY
            }
        }

        return score
    }

    /**
     * Similarity ratio in [0, 1] (1 = identical). Based on Levenshtein distance.
     */
    private fun similarityRatio(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val maxLen = maxOf(a.length, b.length)
        val distance = levenshtein(a, b)
        return 1f - (distance.toFloat() / maxLen)
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}
