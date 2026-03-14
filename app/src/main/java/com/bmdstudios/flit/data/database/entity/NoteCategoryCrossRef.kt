package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table entity for many-to-many relationship between notes and categories.
 */
@Entity(
    tableName = "note_categories",
    primaryKeys = ["note_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_core_id", "category_core_id"], unique = true),
        Index(value = ["note_id"]),
        Index(value = ["category_id"]),
        Index(value = ["is_deleted"])
    ]
)
data class NoteCategoryCrossRef(
    val note_id: Long,
    val category_id: Long,
    val note_core_id: Long? = null,
    val category_core_id: Long? = null,
    val ver: Int = 1,
    val is_deleted: Boolean = false,
    val updated_at: Long
)
