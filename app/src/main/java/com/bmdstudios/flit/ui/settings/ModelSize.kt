package com.bmdstudios.flit.ui.settings

/**
 * Model size preference for the application.
 * Determines the balance between speed and accuracy of the transcription model.
 */
enum class ModelSize {
    /**
     * None - text only mode, no transcription model.
     */
    NONE,

    /**
     * Light model - fast and less accurate, better on older phones.
     */
    LIGHT,

    /**
     * Medium model - balanced speed and accuracy.
     */
    MEDIUM,

    /**
     * Heavy model - slower and more accurate, better on newer phones.
     */
    HEAVY
}
