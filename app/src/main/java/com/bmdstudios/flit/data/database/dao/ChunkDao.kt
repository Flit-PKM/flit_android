package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for chunk operations.
 */
@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getChunkById(id: Long): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE id = :id")
    fun getChunkByIdFlow(id: Long): Flow<ChunkEntity?>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId ORDER BY position_start ASC")
    suspend fun getChunksByNoteId(noteId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId ORDER BY position_start ASC")
    fun getChunksByNoteIdFlow(noteId: Long): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId AND position_start >= :start AND position_end <= :end ORDER BY position_start ASC")
    suspend fun getChunksByNoteIdAndRange(noteId: Long, start: Int, end: Int): List<ChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: ChunkEntity)

    @Delete
    suspend fun deleteChunk(chunk: ChunkEntity)

    @Query("DELETE FROM chunks WHERE id = :id")
    suspend fun deleteChunkById(id: Long)

    @Query("DELETE FROM chunks WHERE note_id = :noteId")
    suspend fun deleteChunksByNoteId(noteId: Long)

    @Query("SELECT COUNT(*) FROM chunks WHERE note_id = :noteId")
    suspend fun getChunkCountByNoteId(noteId: Long): Int
}
