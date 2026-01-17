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
        Index(value = ["note_id"]),
        Index(value = ["category_id"])
    ]
)
data class NoteCategoryCrossRef(
    val note_id: Long,
    val category_id: Long
)
