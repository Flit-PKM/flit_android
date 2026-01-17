package com.bmdstudios.flit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing application settings.
 * Uses DataStore to persist and observe settings changes.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val MODEL_SIZE_KEY = stringPreferencesKey("model_size")
        private const val TAG = "SettingsRepository"
    }

    /**
     * Flow of theme mode preference.
     * Defaults to SYSTEM if no preference is set.
     */
    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE_KEY]
        try {
            themeModeString?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Invalid theme mode stored: $themeModeString, defaulting to SYSTEM")
            ThemeMode.SYSTEM
        }
    }

    /**
     * Gets the current theme mode synchronously.
     * Note: This is a suspend function and should be called from a coroutine.
     */
    suspend fun getThemeMode(): ThemeMode {
        return try {
            val preferences = dataStore.data.first()
            val themeModeString = preferences[THEME_MODE_KEY]
            themeModeString?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        } catch (e: Exception) {
            Timber.e(e, "Error reading theme mode, defaulting to SYSTEM")
            ThemeMode.SYSTEM
        }
    }

    /**
     * Sets the theme mode preference.
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        try {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = themeMode.name
            }
            Timber.d("Theme mode set to: ${themeMode.name}")
        } catch (e: Exception) {
            Timber.e(e, "Error setting theme mode")
        }
    }

    /**
     * Flow of model size preference.
     * Defaults to NONE if no preference is set.
     */
    val modelSizeFlow: Flow<ModelSize> = dataStore.data.map { preferences ->
        val modelSizeString = preferences[MODEL_SIZE_KEY]
        try {
            modelSizeString?.let { ModelSize.valueOf(it) } ?: ModelSize.NONE
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Invalid model size stored: $modelSizeString, defaulting to NONE")
            ModelSize.NONE
        }
    }

    /**
     * Gets the current model size synchronously.
     * Note: This is a suspend function and should be called from a coroutine.
     */
    suspend fun getModelSize(): ModelSize {
        return try {
            val preferences = dataStore.data.first()
            val modelSizeString = preferences[MODEL_SIZE_KEY]
            modelSizeString?.let { ModelSize.valueOf(it) } ?: ModelSize.NONE
        } catch (e: Exception) {
            Timber.e(e, "Error reading model size, defaulting to NONE")
            ModelSize.NONE
        }
    }

    /**
     * Sets the model size preference.
     */
    suspend fun setModelSize(modelSize: ModelSize) {
        try {
            dataStore.edit { preferences ->
                preferences[MODEL_SIZE_KEY] = modelSize.name
            }
            Timber.d("Model size set to: ${modelSize.name}")
        } catch (e: Exception) {
            Timber.e(e, "Error setting model size")
        }
    }

    /**
     * Checks if model size preference has been set (not first startup).
     * Returns true if preference exists, false otherwise.
     */
    suspend fun hasModelSizePreference(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences.contains(MODEL_SIZE_KEY)
        } catch (e: Exception) {
            Timber.e(e, "Error checking model size preference")
            false
        }
    }
}
