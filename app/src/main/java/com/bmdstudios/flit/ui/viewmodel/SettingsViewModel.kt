package com.bmdstudios.flit.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.data.api.FlitApiService
import com.bmdstudios.flit.data.api.model.ConnectExchangeRequest
import com.bmdstudios.flit.data.repository.ExportRepository
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.data.repository.SyncRepository
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State for export operations.
 */
sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val filePath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

/**
 * State for markdown / zip import (merge).
 */
sealed class ImportState {
    object Idle : ImportState()
    object Importing : ImportState()
    data class Success(val notesImported: Int, val relationshipsImported: Int, val relationshipsSkipped: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

/**
 * State for connection operations.
 */
sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connecting : ConnectionState()
    object Success : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * State for sync operations.
 */
sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val currentStep: String, val progress: Float) : SyncState() // progress 0-1
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * ViewModel for managing application settings.
 * Observes settings from the repository and provides state for the UI.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportRepository: ExportRepository,
    private val flitApiService: FlitApiService,
    private val syncRepository: SyncRepository,
    private val appConfig: AppConfig,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** URL for opening Flit Core login in the browser (from [AppConfig]). */
    val flitCoreWebLoginUrl: String = appConfig.flitCoreWebLoginUrl

    /**
     * State flow of the current theme mode.
     */
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    /**
     * State flow of the current model size.
     */
    val modelSize: StateFlow<ModelSize> = settingsRepository.modelSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ModelSize.NONE
        )

    /**
     * State flow of the note details preference.
     */
    val noteDetails: StateFlow<Boolean> = settingsRepository.noteDetailsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * State flow for export operations.
     */
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * State flow for connection operations.
     */
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * State flow for sync operations.
     */
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Flow indicating if the app is connected (has valid tokens).
     */
    val isConnected: StateFlow<Boolean> = settingsRepository.accessTokenFlow
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        Timber.d("SettingsViewModel initialized")
    }

    /**
     * Updates the theme mode preference.
     */
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    /**
     * Updates the model size preference.
     */
    fun setModelSize(modelSize: ModelSize) {
        viewModelScope.launch {
            settingsRepository.setModelSize(modelSize)
        }
    }

    /**
     * Updates the note details preference.
     */
    fun setNoteDetails(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNoteDetails(enabled)
        }
    }

    /**
     * Exports all notes to a zip of markdown files in Downloads.
     */
    fun exportData() {
        viewModelScope.launch {
            try {
                if (!exportRepository.hasStoragePermission()) {
                    _exportState.value = ExportState.Error(
                        "Storage permission is required. Please grant permission in app settings."
                    )
                    return@launch
                }

                _exportState.value = ExportState.Exporting

                exportRepository.exportToZip().fold(
                    onSuccess = { filePath ->
                        _exportState.value = ExportState.Success(filePath)
                    },
                    onFailure = { error ->
                        _exportState.value = ExportState.Error(
                            error.message ?: "Export failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during export")
                _exportState.value = ExportState.Error(
                    "Unexpected error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Merges notes from a zip (root `.md` only) or a single markdown file.
     */
    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Importing
                exportRepository.importFromUri(uri).fold(
                    onSuccess = { result ->
                        _importState.value = ImportState.Success(
                            notesImported = result.notesImported,
                            relationshipsImported = result.relationshipsImported,
                            relationshipsSkipped = result.relationshipsSkipped
                        )
                    },
                    onFailure = { error ->
                        _importState.value = ImportState.Error(
                            error.message ?: "Import failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during import")
                _importState.value = ImportState.Error(
                    "Unexpected error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Resets export state to idle.
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    /**
     * Connects to Flit Core by exchanging a connection code for tokens.
     */
    fun connectToFlitCore(connectionCode: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting

                // Get device metadata
                val deviceName = Build.MODEL.ifBlank { "Android Device" }
                val platform = getPlatformString()
                val appVersion = getAppVersion()

                val request = ConnectExchangeRequest(
                    connectionCode = connectionCode.trim(),
                    appSlug = "flit",
                    deviceName = deviceName,
                    platform = platform,
                    appVersion = appVersion
                )

                val result = flitApiService.exchangeConnectionCode(request)

                result.fold(
                    onSuccess = { response ->
                        // Save tokens
                        settingsRepository.saveTokens(
                            accessToken = response.accessToken,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                        Timber.d("Successfully connected to Flit Core")
                        _connectionState.value = ConnectionState.Success
                    },
                    onFailure = { throwable ->
                        val error = throwable.toAppError("connectToFlitCore")
                        val errorMessage = ErrorHandler.handleError(error)
                        Timber.e(throwable, "Failed to connect to Flit Core")
                        _connectionState.value = ConnectionState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during connection")
                val error = ErrorHandler.transform(e, "connectToFlitCore")
                val errorMessage = ErrorHandler.handleError(error)
                _connectionState.value = ConnectionState.Error(errorMessage)
            }
        }
    }

    /**
     * Resets connection state to idle.
     */
    fun resetConnectionState() {
        _connectionState.value = ConnectionState.Idle
    }

    /**
     * Syncs data with Flit Core backend.
     * Updates sync state with progress steps.
     */
    fun sync() {
        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing("Starting sync...", 0f)
                
                _syncState.value = SyncState.Syncing("Comparing notes...", 0.1f)
                val result = syncRepository.runSync()
                
                when (result) {
                    is SyncRepository.SyncResult.NotAuthenticated -> {
                        _syncState.value = SyncState.Error("Not authenticated. Please connect to Flit - Core first.")
                    }
                    is SyncRepository.SyncResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                    }
                    is SyncRepository.SyncResult.Success -> {
                        _syncState.value = SyncState.Success
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during sync")
                val error = ErrorHandler.transform(e, "sync")
                val errorMessage = ErrorHandler.handleError(error)
                _syncState.value = SyncState.Error(errorMessage)
            }
        }
    }

    /**
     * Resets sync state to idle.
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Gets the app version from package manager.
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "1.0"
        } catch (e: Exception) {
            Timber.w(e, "Failed to get app version, using default")
            "1.0"
        }
    }

    /**
     * Gets a descriptive platform string including Android version.
     * Format: "Android {VERSION} (API {SDK_INT})"
     * Example: "Android 13 (API 33)" or "Android 14 (API 34)"
     */
    private fun getPlatformString(): String {
        val versionRelease = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        return "Android $versionRelease (API $sdkInt)"
    }
}
