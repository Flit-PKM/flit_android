package com.bmdstudios.flit.data.search

import com.bmdstudios.flit.data.database.entity.NoteSearchRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NoteSearchScorer].
 */
class NoteSearchScorerTest {

    private fun row(noteId: Long, content: String, updatedAt: Long = 1000L) =
        NoteSearchRow(noteId = noteId, content = content, updatedAt = updatedAt)

    @Test
    fun rank_emptyQuery_returnsByRecency() {
        val candidates = listOf(
            row(1, "apple banana", 100),
            row(2, "cherry", 200)
        )
        val result = NoteSearchScorer.rank(emptyList(), candidates)
        assertEquals(listOf(2L, 1L), result)
    }

    @Test
    fun rank_prefixMatch_scoresHigher() {
        val candidates = listOf(
            row(1, "apple banana", 100),
            row(2, "application", 200)
        )
        val result = NoteSearchScorer.rank(listOf("app"), candidates)
        assertTrue(result.isNotEmpty())
        assertTrue(result.first() in listOf(1L, 2L))
    }

    @Test
    fun rank_substringMatch_included() {
        val candidates = listOf(
            row(1, "hello world", 100)
        )
        val result = NoteSearchScorer.rank(listOf("wor"), candidates)
        assertEquals(listOf(1L), result)
    }

    @Test
    fun rank_exactPhrase_ranksFirst() {
        val candidates = listOf(
            row(1, "quick brown fox", 100),
            row(2, "quick brown", 200)
        )
        val result = NoteSearchScorer.rank(listOf("quick", "brown"), candidates)
        assertTrue(result.isNotEmpty())
        assertTrue(result.first() in listOf(1L, 2L))
    }

    @Test
    fun rank_multipleCandidates_sortedByScoreThenRecency_excludesNoMatch() {
        val candidates = listOf(
            row(1, "apple only", 100),
            row(2, "apple banana", 200),
            row(3, "orange", 300)
        )
        val result = NoteSearchScorer.rank(listOf("apple"), candidates)
        assertTrue(3L !in result)
        assertTrue(1L in result)
        assertTrue(2L in result)
        assertTrue(result.indexOf(2L) < result.indexOf(1L))
    }

    @Test
    fun rank_noMatch_returnsEmpty() {
        val candidates = listOf(
            row(1, "apple banana", 100),
            row(2, "other", 200)
        )
        val result = NoteSearchScorer.rank(listOf("xyz"), candidates)
        assertEquals(emptyList<Long>(), result)
    }
}
