package com.bmdstudios.flit.data.database.entity

/**
 * Result of joining notesearch with notes to get updated_at for ranking.
 * Used by search to score and sort by recency.
 */
data class NoteSearchRow(
    val note_id: Long,
    val content: String,
    val updated_at: Long
)
