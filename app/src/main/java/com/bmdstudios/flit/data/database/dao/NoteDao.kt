package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note operations.
 */
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE status = :status ORDER BY created_at DESC")
    suspend fun getNotesByStatus(status: NoteStatus): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE status = :status ORDER BY created_at DESC")
    fun getNotesByStatusFlow(status: NoteStatus): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC")
    suspend fun getNotesByDateRange(startDate: Long, endDate: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchNotesFlow(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE status = :status")
    suspend fun getNoteCountByStatus(status: NoteStatus): Int
}
