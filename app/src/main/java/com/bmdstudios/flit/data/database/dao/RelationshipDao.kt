package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.RelationshipType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for relationship operations.
 */
@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationships WHERE id = :id")
    suspend fun getRelationshipById(id: Long): RelationshipEntity?

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteId OR note_b_id = :noteId ORDER BY created_at DESC")
    suspend fun getRelationshipsByNoteId(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteId OR note_b_id = :noteId ORDER BY created_at DESC")
    fun getRelationshipsByNoteIdFlow(noteId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteId ORDER BY created_at DESC")
    suspend fun getRelationshipsFromNote(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_b_id = :noteId ORDER BY created_at DESC")
    suspend fun getRelationshipsToNote(noteId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE (note_a_id = :noteId OR note_b_id = :noteId) AND type = :type ORDER BY created_at DESC")
    suspend fun getRelationshipsByNoteIdAndType(noteId: Long, type: RelationshipType): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE type = :type ORDER BY created_at DESC")
    suspend fun getRelationshipsByType(type: RelationshipType): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteAId AND note_b_id = :noteBId")
    suspend fun getRelationshipBetweenNotes(noteAId: Long, noteBId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE note_a_id = :noteAId AND note_b_id = :noteBId AND type = :type")
    suspend fun getRelationshipBetweenNotesWithType(noteAId: Long, noteBId: Long, type: RelationshipType): RelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<RelationshipEntity>)

    @Update
    suspend fun updateRelationship(relationship: RelationshipEntity)

    @Delete
    suspend fun deleteRelationship(relationship: RelationshipEntity)

    @Query("DELETE FROM relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: Long)

    @Query("DELETE FROM relationships WHERE note_a_id = :noteId OR note_b_id = :noteId")
    suspend fun deleteRelationshipsByNoteId(noteId: Long)

    @Query("SELECT COUNT(*) FROM relationships WHERE note_a_id = :noteId OR note_b_id = :noteId")
    suspend fun getRelationshipCountByNoteId(noteId: Long): Int
}
