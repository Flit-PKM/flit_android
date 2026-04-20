package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note operations.
 * Visible queries exclude rows with is_deleted = true.
 */
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE id = :id AND is_deleted = 0")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id AND is_deleted = 0")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND workflow_status = :status ORDER BY created_at DESC")
    suspend fun getNotesByWorkflowStatus(status: NoteStatus): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND workflow_status = :status ORDER BY created_at DESC")
    fun getNotesByWorkflowStatusFlow(status: NoteStatus): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC")
    suspend fun getNotesByDateRange(startDate: Long, endDate: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE id = :id")
    suspend fun softDeleteNoteById(id: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM notes WHERE is_deleted = 0")
    suspend fun getNoteCount(): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getTotalNoteCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE is_deleted = 0 AND workflow_status = :status")
    suspend fun getNoteCountByWorkflowStatus(status: NoteStatus): Int

    @Query("DELETE FROM notes WHERE is_deleted = 1 AND updated_at < :cutoffMs")
    suspend fun purgeDeletedOlderThan(cutoffMs: Long)

    /** All notes including soft-deleted, for building sync compare payload. */
    @Query("SELECT * FROM notes ORDER BY id")
    suspend fun getAllNotesForSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE core_id = :coreId LIMIT 1")
    suspend fun getNoteByCoreId(coreId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE core_id IN (:coreIds)")
    suspend fun getNotesByCoreIds(coreIds: List<Long>): List<NoteEntity>

    /** Fetches notes by IDs. Order of result is not guaranteed; caller should sort by desired order. */
    @Query("SELECT * FROM notes WHERE id IN (:ids) AND is_deleted = 0")
    suspend fun getNotesByIds(ids: List<Long>): List<NoteEntity>

    @Query(
        """
        INSERT INTO notes(
            id, core_id, ver, is_deleted, title, text, recording, embedding_vector, created_at, updated_at, workflow_status
        ) VALUES(
            :id, NULL, :ver, 0, :title, :text, NULL, NULL, :createdAt, :updatedAt, :workflowStatus
        )
        """
    )
    suspend fun insertWelcomeNoteWithId(
        id: Long,
        ver: Int,
        title: String,
        text: String,
        createdAt: Long,
        updatedAt: Long,
        workflowStatus: String
    )
}
