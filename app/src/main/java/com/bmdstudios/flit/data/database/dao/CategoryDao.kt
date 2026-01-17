package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for category operations.
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE id = :id")
    fun getCategoryByIdFlow(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM category WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM category ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM category ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchCategories(query: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun getCategoryCount(): Int
}
