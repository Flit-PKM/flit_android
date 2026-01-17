package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user settings operations.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM user WHERE id = 1")
    suspend fun deleteUser()
}
