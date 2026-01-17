package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity

/**
 * User entity for storing user settings.
 * Only one entry should exist (id = 1).
 */
@Entity(
    tableName = "user",
    primaryKeys = ["id"]
)
data class UserEntity(
    val id: Int = 1,
    val settings: String? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null
) {
    init {
        require(id == 1) { "UserEntity id must be 1" }
    }
}
