package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for chunk operations.
 * Visible queries exclude rows with is_deleted = true.
 */
@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE id = :id AND is_deleted = 0")
    suspend fun getChunkById(id: Long): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE id = :id AND is_deleted = 0")
    fun getChunkByIdFlow(id: Long): Flow<ChunkEntity?>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId AND is_deleted = 0 ORDER BY position_start ASC")
    suspend fun getChunksByNoteId(noteId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId AND is_deleted = 0 ORDER BY position_start ASC")
    fun getChunksByNoteIdFlow(noteId: Long): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId AND is_deleted = 0 AND position_start >= :start AND position_end <= :end ORDER BY position_start ASC")
    suspend fun getChunksByNoteIdAndRange(noteId: Long, start: Int, end: Int): List<ChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: ChunkEntity)

    @Query("UPDATE chunks SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE id = :id")
    suspend fun softDeleteChunkById(id: Long, updatedAt: Long)

    @Query("UPDATE chunks SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE note_id = :noteId")
    suspend fun softDeleteChunksByNoteId(noteId: Long, updatedAt: Long)

    @Query("DELETE FROM chunks WHERE id = :id")
    suspend fun deleteChunkById(id: Long)

    @Query("DELETE FROM chunks WHERE note_id = :noteId")
    suspend fun deleteChunksByNoteId(noteId: Long)

    @Query("SELECT COUNT(*) FROM chunks WHERE note_id = :noteId AND is_deleted = 0")
    suspend fun getChunkCountByNoteId(noteId: Long): Int

    @Query("DELETE FROM chunks WHERE is_deleted = 1 AND updated_at < :cutoffMs")
    suspend fun purgeDeletedOlderThan(cutoffMs: Long)

    /** All chunks including soft-deleted, for building sync compare payload. */
    @Query("SELECT * FROM chunks ORDER BY id")
    suspend fun getAllChunksForSync(): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE core_id = :coreId LIMIT 1")
    suspend fun getChunkByCoreId(coreId: Long): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE core_id IN (:coreIds)")
    suspend fun getChunksByCoreIds(coreIds: List<Long>): List<ChunkEntity>
}
