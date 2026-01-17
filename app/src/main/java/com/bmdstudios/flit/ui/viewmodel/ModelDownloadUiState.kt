package com.bmdstudios.flit.ui.viewmodel

sealed class DownloadUiState {
    object Idle : DownloadUiState()
    data class Downloading(
        val progress: Float,
        val currentFile: String
    ) : DownloadUiState()
    object Success : DownloadUiState()
    data class Error(val message: String) : DownloadUiState()
}
