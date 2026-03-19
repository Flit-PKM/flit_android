package com.bmdstudios.flit.data.database.entity

import androidx.room.ColumnInfo

/**
 * Result of joining notesearch with notes to get updated_at for ranking.
 * Used by search to score and sort by recency.
 */
data class NoteSearchRow(
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    val content: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
