package com.bmdstudios.flit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing application settings.
 * Observes settings from the repository and provides state for the UI.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
}
