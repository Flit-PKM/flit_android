package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bmdstudios.flit.data.database.model.RelationshipType

/**
 * Relationship entity for knowledge graph connections between notes.
 */
@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_a_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_b_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_a_core_id", "note_b_core_id"], unique = true),
        Index(value = ["note_a_id"]),
        Index(value = ["note_b_id"]),
        Index(value = ["type"]),
        Index(value = ["note_a_id", "note_b_id", "type"], unique = true),
        Index(value = ["is_deleted"])
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val note_a_core_id: Long? = null,
    val note_b_core_id: Long? = null,
    val ver: Int = 1,
    val is_deleted: Boolean = false,
    val note_a_id: Long,
    val note_b_id: Long,
    val type: RelationshipType,
    val created_at: Long,
    val updated_at: Long
)
