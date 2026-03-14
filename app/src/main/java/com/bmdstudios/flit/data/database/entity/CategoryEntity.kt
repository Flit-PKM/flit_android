package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category entity for tag descriptions.
 */
@Entity(
    tableName = "category",
    indices = [
        Index(value = ["core_id"], unique = true),
        Index(value = ["name"], unique = true),
        Index(value = ["is_deleted"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val core_id: Long? = null,
    val ver: Int = 1,
    val is_deleted: Boolean = false,
    val name: String,
    val created_at: Long,
    val updated_at: Long
)
