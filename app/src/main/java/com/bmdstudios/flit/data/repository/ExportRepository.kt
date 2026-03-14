package com.bmdstudios.flit.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.bmdstudios.flit.data.database.FlitDatabase
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.export.ExportData
import com.bmdstudios.flit.data.export.ExportMetadata
import com.bmdstudios.flit.data.export.toExportCategory
import com.bmdstudios.flit.data.export.toExportNote
import com.bmdstudios.flit.data.export.toExportNoteCategory
import com.bmdstudios.flit.data.export.toExportRelationship
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for exporting app data to JSON format.
 * Handles data gathering, serialization, and file writing.
 */
@Singleton
class ExportRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val relationshipDao: RelationshipDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val database: FlitDatabase,
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Gathers all data from the database for export.
     */
    suspend fun getAllExportData(): Result<ExportData> = withContext(Dispatchers.IO) {
        try {
            val notes = noteDao.getAllNotes().map { it.toExportNote() }
            val categories = categoryDao.getAllCategories().map { it.toExportCategory() }
            val relationships = relationshipDao.getAllRelationships().map { it.toExportRelationship() }
            val noteCategories = noteCategoryDao.getAllNoteCategories().map { it.toExportNoteCategory() }

            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }
            val appVersion = packageInfo?.versionName ?: "Unknown"
            
            // Get database version from Room database
            val dbVersion = database.openHelper.readableDatabase.version
            
            val metadata = ExportMetadata(
                timestamp = System.currentTimeMillis(),
                app_version = appVersion,
                database_version = dbVersion
            )

            val exportData = ExportData(
                export_metadata = metadata,
                notes = notes,
                categories = categories,
                relationships = relationships,
                note_categories = noteCategories
            )

            Result.success(exportData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to gather export data")
            Result.failure(e)
        }
    }

    /**
     * Exports data to JSON file in system Downloads folder.
     * Returns the file path on success.
     */
    suspend fun exportToJson(exportData: ExportData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(exportData)
            val fileName = generateFileName()

            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore to write to system Downloads folder
                exportToDownloadsViaMediaStore(fileName, jsonString)
            } else {
                // Android 9 and below: Use legacy approach with public Downloads folder
                exportToDownloadsLegacy(fileName, jsonString)
            }

            Timber.d("Export successful: $filePath")
            Result.success(filePath)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied while exporting")
            Result.failure(Exception("Permission denied. Please grant storage permission.", e))
        } catch (e: Exception) {
            Timber.e(e, "Failed to export data to JSON")
            Result.failure(Exception("Failed to write export file: ${e.message}", e))
        }
    }

    /**
     * Generates a unique filename with timestamp.
     */
    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "flit_export_$timestamp.json"
    }

    /**
     * Exports file to system Downloads folder using MediaStore API (Android 10+).
     * Returns the file path string for display purposes.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun exportToDownloadsViaMediaStore(fileName: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw Exception("Failed to create file in Downloads folder")

        context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw Exception("Failed to open output stream for file")

        // Return a user-friendly path string
        return "Downloads/$fileName"
    }

    /**
     * Exports file to public Downloads folder using legacy approach (Android 9 and below).
     * Returns the absolute file path.
     */
    private fun exportToDownloadsLegacy(fileName: String, content: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        // Ensure parent directory exists
        file.parentFile?.mkdirs()

        // Write JSON to file
        FileWriter(file).use { writer ->
            writer.write(content)
        }

        return file.absolutePath
    }

    /**
     * Checks if storage permissions are granted.
     * For Android 10+, MediaStore Downloads doesn't require special permissions.
     * For Android 9 and below, WRITE_EXTERNAL_STORAGE permission is required.
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10+ doesn't require WRITE_EXTERNAL_STORAGE for MediaStore Downloads
            true
        }
    }
}
