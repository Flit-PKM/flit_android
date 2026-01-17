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
        Index(value = ["note_a_id"]),
        Index(value = ["note_b_id"]),
        Index(value = ["type"]),
        Index(value = ["note_a_id", "note_b_id", "type"], unique = true)
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val note_a_id: Long,
    val note_b_id: Long,
    val type: RelationshipType,
    val created_at: Long
)
