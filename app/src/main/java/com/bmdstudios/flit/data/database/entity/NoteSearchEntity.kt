package com.bmdstudios.flit.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Search index for notes. One row per note; content is normalized
 * (lowercase, stop words removed) title + body. Not used in sync;
 * row is hard-deleted when the note is soft-deleted.
 */
@Entity(tableName = "notesearch")
data class NoteSearchEntity(
    @PrimaryKey
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    val content: String
)
