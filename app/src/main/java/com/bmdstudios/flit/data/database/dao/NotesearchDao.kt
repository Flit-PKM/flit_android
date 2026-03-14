package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bmdstudios.flit.data.database.entity.NoteSearchEntity
import com.bmdstudios.flit.data.database.entity.NoteSearchRow

/**
 * DAO for the notesearch table. Used for ranked full-text search only;
 * not synced. Rows are hard-deleted when the corresponding note is soft-deleted.
 */
@Dao
interface NotesearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteSearchEntity)

    @Query("DELETE FROM notesearch WHERE note_id = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    /** All searchable rows (only non-deleted notes have rows). */
    @Query("SELECT * FROM notesearch")
    suspend fun getAll(): List<NoteSearchEntity>

    /**
     * All searchable rows with updated_at for ranking. Joins with notes.
     */
    @Query(
        """
        SELECT ns.note_id AS note_id, ns.content AS content, n.updated_at AS updated_at
        FROM notesearch ns INNER JOIN notes n ON n.id = ns.note_id
        """
    )
    suspend fun getAllWithUpdatedAt(): List<NoteSearchRow>

    /**
     * Delete notesearch rows whose note_id is no longer in notes.
     * Optional consistency step during purge.
     */
    @Query("DELETE FROM notesearch WHERE note_id NOT IN (SELECT id FROM notes)")
    suspend fun deleteOrphans()
}
