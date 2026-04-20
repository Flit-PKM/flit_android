package com.bmdstudios.flit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bmdstudios.flit.config.AppConfig
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
        private val NOTE_DETAILS_KEY = booleanPreferencesKey("note_details")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val TOKEN_EXPIRES_AT_KEY = stringPreferencesKey("token_expires_at")
        private val ONBOARDING_REVISION_COMPLETED_KEY = intPreferencesKey("onboarding_revision_completed")
        private val INSTALL_WELCOME_SEED_ATTEMPTED_KEY = booleanPreferencesKey("install_welcome_seed_attempted")
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

    /**
     * Flow of note details preference.
     * Defaults to false (show only title) if no preference is set.
     */
    val noteDetailsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTE_DETAILS_KEY] ?: false
    }

    /**
     * Gets the current note details preference synchronously.
     * Note: This is a suspend function and should be called from a coroutine.
     */
    suspend fun getNoteDetails(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[NOTE_DETAILS_KEY] ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error reading note details preference, defaulting to false")
            false
        }
    }

    /**
     * Sets the note details preference.
     */
    suspend fun setNoteDetails(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[NOTE_DETAILS_KEY] = enabled
            }
            Timber.d("Note details set to: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Error setting note details preference")
        }
    }

    /**
     * Saves access and refresh tokens along with expiration time.
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        try {
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
            dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = accessToken
                preferences[REFRESH_TOKEN_KEY] = refreshToken
                preferences[TOKEN_EXPIRES_AT_KEY] = expiresAt.toString()
            }
            Timber.d("Tokens saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error saving tokens")
        }
    }

    /**
     * Flow of access token.
     * Returns null if no token is stored.
     */
    val accessTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    /**
     * Gets the current access token synchronously.
     */
    suspend fun getAccessToken(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[ACCESS_TOKEN_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Error reading access token")
            null
        }
    }

    /**
     * Flow of refresh token.
     * Returns null if no token is stored.
     */
    val refreshTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    /**
     * Gets the current refresh token synchronously.
     */
    suspend fun getRefreshToken(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[REFRESH_TOKEN_KEY]
        } catch (e: Exception) {
            Timber.e(e, "Error reading refresh token")
            null
        }
    }

    /**
     * Checks if a valid access token exists (not expired).
     */
    suspend fun hasValidToken(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            val accessToken = preferences[ACCESS_TOKEN_KEY]
            val expiresAtString = preferences[TOKEN_EXPIRES_AT_KEY]
            
            if (accessToken == null || expiresAtString == null) {
                false
            } else {
                val expiresAt = expiresAtString.toLongOrNull() ?: return false
                System.currentTimeMillis() < expiresAt
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking token validity")
            false
        }
    }

    /**
     * Clears all stored tokens.
     */
    suspend fun clearTokens() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
                preferences.remove(REFRESH_TOKEN_KEY)
                preferences.remove(TOKEN_EXPIRES_AT_KEY)
            }
            Timber.d("Tokens cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing tokens")
        }
    }

    suspend fun getOnboardingRevisionCompleted(): Int {
        return try {
            val preferences = dataStore.data.first()
            preferences[ONBOARDING_REVISION_COMPLETED_KEY] ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Error reading onboarding revision completed")
            0
        }
    }

    suspend fun setOnboardingRevisionCompleted(revision: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[ONBOARDING_REVISION_COMPLETED_KEY] = revision
            }
            Timber.tag(TAG).d("Onboarding revision completed set to $revision")
        } catch (e: Exception) {
            Timber.e(e, "Error setting onboarding revision completed")
        }
    }

    suspend fun shouldShowOnboarding(): Boolean {
        return getOnboardingRevisionCompleted() < AppConfig.ONBOARDING_REVISION
    }

    suspend fun hasAttemptedInstallWelcomeSeed(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[INSTALL_WELCOME_SEED_ATTEMPTED_KEY] ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error reading install welcome seed flag")
            false
        }
    }

    suspend fun setInstallWelcomeSeedAttempted(attempted: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[INSTALL_WELCOME_SEED_ATTEMPTED_KEY] = attempted
            }
            Timber.tag(TAG).d("Install welcome seed attempted flag set to $attempted")
        } catch (e: Exception) {
            Timber.e(e, "Error setting install welcome seed flag")
        }
    }
}
