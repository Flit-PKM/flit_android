package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for category operations.
 * Visible queries exclude rows with is_deleted = true.
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE id = :id AND is_deleted = 0")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE id = :id AND is_deleted = 0")
    fun getCategoryByIdFlow(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM category WHERE name = :name AND is_deleted = 0 LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM category WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE is_deleted = 0 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchCategories(query: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE category SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE id = :id")
    suspend fun softDeleteCategoryById(id: Long, updatedAt: Long)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM category WHERE is_deleted = 0")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM category WHERE is_deleted = 1 AND updated_at < :cutoffMs")
    suspend fun purgeDeletedOlderThan(cutoffMs: Long)

    /** All categories including soft-deleted, for building sync compare payload. */
    @Query("SELECT * FROM category ORDER BY id")
    suspend fun getAllCategoriesForSync(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE core_id = :coreId LIMIT 1")
    suspend fun getCategoryByCoreId(coreId: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE core_id IN (:coreIds)")
    suspend fun getCategoriesByCoreIds(coreIds: List<Long>): List<CategoryEntity>
}
