package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.RelationshipType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for relationship operations.
 * Visible queries exclude rows with is_deleted = true.
 */
@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationships WHERE id = :id AND is_deleted = 0")
    suspend fun getRelationshipById(id: Long): RelationshipEntity?

    @Query("SELECT * FROM relationships WHERE (note_a_id = :noteId OR note_b_id = :noteId) AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getRelationshipsByNoteId(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE (note_a_id = :noteId OR note_b_id = :noteId) AND is_deleted = 0 ORDER BY created_at DESC")
    fun getRelationshipsByNoteIdFlow(noteId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getRelationshipsFromNote(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_b_id = :noteId AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getRelationshipsToNote(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE (note_a_id = :noteId OR note_b_id = :noteId) AND type = :type AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getRelationshipsByNoteIdAndType(noteId: Long, type: RelationshipType): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE type = :type AND is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getRelationshipsByType(type: RelationshipType): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteAId AND note_b_id = :noteBId AND is_deleted = 0")
    suspend fun getRelationshipBetweenNotes(noteAId: Long, noteBId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteAId AND note_b_id = :noteBId AND type = :type AND is_deleted = 0")
    suspend fun getRelationshipBetweenNotesWithType(noteAId: Long, noteBId: Long, type: RelationshipType): RelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<RelationshipEntity>)

    @Update
    suspend fun updateRelationship(relationship: RelationshipEntity)

    @Query("UPDATE relationships SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE id = :id")
    suspend fun softDeleteRelationshipById(id: Long, updatedAt: Long)

    @Query("UPDATE relationships SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE note_a_id = :noteId OR note_b_id = :noteId")
    suspend fun softDeleteRelationshipsByNoteId(noteId: Long, updatedAt: Long)

    @Query("DELETE FROM relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: Long)

    @Query("DELETE FROM relationships WHERE note_a_id = :noteId OR note_b_id = :noteId")
    suspend fun deleteRelationshipsByNoteId(noteId: Long)

    @Query("SELECT COUNT(*) FROM relationships WHERE (note_a_id = :noteId OR note_b_id = :noteId) AND is_deleted = 0")
    suspend fun getRelationshipCountByNoteId(noteId: Long): Int

    @Query("SELECT * FROM relationships WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllRelationships(): List<RelationshipEntity>

    @Query("DELETE FROM relationships WHERE is_deleted = 1 AND updated_at < :cutoffMs")
    suspend fun purgeDeletedOlderThan(cutoffMs: Long)

    /** All relationships including soft-deleted, for building sync compare payload. */
    @Query("SELECT * FROM relationships ORDER BY id")
    suspend fun getAllRelationshipsForSync(): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_core_id = :noteACoreId AND note_b_core_id = :noteBCoreId LIMIT 1")
    suspend fun getRelationshipByCorePair(noteACoreId: Long, noteBCoreId: Long): RelationshipEntity?

    /** Lookup by local note_a_id/note_b_id for sync push; includes soft-deleted. */
    @Query("SELECT * FROM relationships WHERE note_a_id = :noteAId AND note_b_id = :noteBId ORDER BY id LIMIT 1")
    suspend fun getRelationshipByLocalNotePairForSync(noteAId: Long, noteBId: Long): RelationshipEntity?
}
