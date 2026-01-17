package com.bmdstudios.flit.utils.model

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart file matcher for model files.
 * Parses filenames to identify components and automatically prioritizes int8 models.
 * Works with all model types (Whisper, SenseVoice, Transducer, etc.).
 */
@Singleton
class FileMatcher @Inject constructor() {

    /**
     * Finds a model file matching the required component (encoder/decoder/joiner/model/tokens).
     * Automatically prioritizes int8 variants when both are available (for .onnx files).
     *
     * @param files List of file names to search
     * @param mustContain Required component name (e.g., "encoder", "decoder", "joiner", "model", "tokens")
     * @param prioritizeInt8 If true, prefer int8 models over regular models (only relevant for .onnx files)
     * @param extension File extension to match (default: "onnx")
     * @return Matching file name, or null if not found
     */
    fun findModelFile(
        files: List<String>,
        mustContain: String,
        prioritizeInt8: Boolean = true,
        extension: String = ModelConstants.ONNX_EXTENSION
    ): String? {
        val candidates = files.mapNotNull { filename ->
            parseModelFile(filename, mustContain, extension)
        }

        if (candidates.isEmpty()) {
            return null
        }

        // Sort: int8 files first if prioritizing, then by filename for consistency
        val sorted = if (prioritizeInt8) {
            candidates.sortedWith(
                compareBy<ModelFileInfo> { !it.hasInt8 } // int8 first (false < true)
                    .thenBy { it.filename } // then alphabetical
            )
        } else {
            candidates.sortedBy { it.filename }
        }

        return sorted.first().filename
    }

    /**
     * Checks if a model file matching the required component exists.
     * Convenience method for boolean existence checks.
     *
     * @param files List of file names to search
     * @param mustContain Required component name (e.g., "encoder", "decoder", "tokens")
     * @param extension File extension to match (default: "onnx")
     * @return true if a matching file exists, false otherwise
     */
    fun hasModelFile(
        files: List<String>,
        mustContain: String,
        extension: String = ModelConstants.ONNX_EXTENSION
    ): Boolean {
        return findModelFile(files, mustContain, prioritizeInt8 = false, extension) != null
    }

    /**
     * Parses a filename to extract model file information.
     *
     * @param filename File name to parse
     * @param mustContain Required component name (e.g., "encoder", "decoder", "tokens")
     * @param extension File extension to match (e.g., "onnx", "txt")
     * @return ModelFileInfo if file matches requirements, null otherwise
     */
    private fun parseModelFile(filename: String, mustContain: String, extension: String): ModelFileInfo? {
        val lowerFilename = filename.lowercase()
        val lowerExtension = extension.lowercase()

        // Must contain required component
        if (!lowerFilename.contains(mustContain.lowercase())) {
            return null
        }

        // Must match the specified extension
        if (!lowerFilename.endsWith(".$lowerExtension")) {
            return null
        }

        // Parse filename components by splitting on '.'
        // Example: "base-encoder.int8.onnx" -> ["base-encoder", "int8", "onnx"]
        val components = filename.split('.')

        // Check if int8 is present as a component
        val hasInt8 = components.any { it.equals("int8", ignoreCase = true) }

        return ModelFileInfo(
            filename = filename,
            hasInt8 = hasInt8
        )
    }

    /**
     * Information about a parsed model file.
     */
    private data class ModelFileInfo(
        val filename: String,
        val hasInt8: Boolean
    )
}
