package com.bmdstudios.flit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing categories.
 * Observes categories from the database and provides state for the UI.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    /**
     * State flow of all categories, ordered by name (ascending).
     */
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        Timber.d("CategoriesViewModel initialized")
    }

    /**
     * Creates a new category with the given name.
     */
    suspend fun createCategory(name: String) {
        withContext(Dispatchers.IO) {
            try {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    Timber.w("Attempted to create category with empty name")
                    return@withContext
                }

                // Check if category with same name already exists
                val existingCategory = categoryDao.getCategoryByName(trimmedName)
                if (existingCategory != null) {
                    Timber.w("Category with name '$trimmedName' already exists")
                    throw IllegalArgumentException("A category with this name already exists")
                }

                val now = System.currentTimeMillis()
                val category = CategoryEntity(
                    name = trimmedName,
                    created_at = now,
                    updated_at = now
                )
                val categoryId = categoryDao.insertCategory(category)
                Timber.i("Category created successfully: $categoryId - $trimmedName")
                syncScheduler.scheduleSyncAfterMutation()
            } catch (e: Exception) {
                Timber.e(e, "Error creating category: $name")
                throw e
            }
        }
    }

    /**
     * Updates an existing category.
     */
    suspend fun updateCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            try {
                val trimmedName = category.name.trim()
                if (trimmedName.isEmpty()) {
                    Timber.w("Attempted to update category with empty name")
                    return@withContext
                }

                // Check if another category with same name already exists
                val existingCategory = categoryDao.getCategoryByName(trimmedName)
                if (existingCategory != null && existingCategory.id != category.id) {
                    Timber.w("Category with name '$trimmedName' already exists")
                    throw IllegalArgumentException("A category with this name already exists")
                }

                val updatedCategory = category.copy(
                    name = trimmedName,
                    updated_at = System.currentTimeMillis(),
                    ver = category.ver + 1
                )
                categoryDao.updateCategory(updatedCategory)
                Timber.i("Category updated successfully: ${category.id} - $trimmedName")
                syncScheduler.scheduleSyncAfterMutation()
            } catch (e: Exception) {
                Timber.e(e, "Error updating category: ${category.id}")
                throw e
            }
        }
    }

    /**
     * Soft-deletes a category (sets is_deleted = true).
     */
    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            try {
                categoryDao.softDeleteCategoryById(category.id, System.currentTimeMillis())
                Timber.i("Category deleted successfully: ${category.id} - ${category.name}")
                syncScheduler.scheduleSyncAfterMutation()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting category: ${category.id}")
                throw e
            }
        }
    }
}
