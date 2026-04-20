package com.bmdstudios.flit.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.bmdstudios.flit.data.database.FlitDatabase
import com.bmdstudios.flit.data.database.NoteWriter
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.database.model.RelationshipType
import com.bmdstudios.flit.data.export.ParsedMarkdownNote
import com.bmdstudios.flit.data.export.buildNoteMarkdownFile
import com.bmdstudios.flit.data.export.isSymmetricForImportDedupe
import com.bmdstudios.flit.data.export.noteMarkdownZipEntryName
import com.bmdstudios.flit.data.export.parseMarkdownNoteFile
import com.bmdstudios.flit.data.export.resolveRelationshipLabel
import com.bmdstudios.flit.data.export.validateZipOnlyMarkdownPaths
import com.bmdstudios.flit.data.export.zipEntryShouldRead
import com.bmdstudios.flit.data.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class MarkdownImportResult(
    val notesImported: Int,
    val relationshipsImported: Int,
    val relationshipsSkipped: Int
)

/**
 * Export notes as a zip of markdown files; merge-import from zip or single markdown via SAF.
 */
@Singleton
class ExportRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val relationshipDao: RelationshipDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val database: FlitDatabase,
    private val noteWriter: NoteWriter,
    private val syncScheduler: SyncScheduler,
    @ApplicationContext private val context: Context
) {

    /**
     * Builds a zip of all notes (markdown) and writes it to Downloads.
     */
    suspend fun exportToZip(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val notes = noteDao.getAllNotes()
            if (notes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No notes to export"))
            }
            val notesById = notes.associateBy { it.id }
            val zipBytes = ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zos ->
                    for (note in notes) {
                        val categories = noteCategoryDao.getCategoriesForNote(note.id).map { it.name }
                        val rels = relationshipDao.getRelationshipsByNoteId(note.id)
                        val md = buildNoteMarkdownFile(note, categories, rels, notesById)
                        val entryName = noteMarkdownZipEntryName(note)
                        zos.putNextEntry(ZipEntry(entryName))
                        zos.write(md.toByteArray(StandardCharsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                baos.toByteArray()
            }
            val fileName = generateZipFileName()
            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportBytesToDownloadsMediaStore(fileName, "application/zip", zipBytes)
            } else {
                exportBytesToDownloadsLegacy(fileName, zipBytes)
            }
            Timber.d("Export successful: $filePath")
            Result.success(filePath)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied while exporting")
            Result.failure(Exception("Permission denied. Please grant storage permission.", e))
        } catch (e: Exception) {
            Timber.e(e, "Failed to export zip")
            Result.failure(Exception("Failed to write export file: ${e.message}", e))
        }
    }

    /**
     * Merges notes from a zip (only root `.md` entries) or a single `.md` file.
     * Relationships only attach to other notes in the same import batch (matching wikilink keys).
     */
    suspend fun importFromUri(uri: Uri): Result<MarkdownImportResult> = withContext(Dispatchers.IO) {
        try {
            val parsed = loadParsedNotesFromUri(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not read import file"))
            if (parsed.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No markdown notes found"))
            }
            val result = runMarkdownMergeImport(parsed)
            syncScheduler.scheduleSyncAfterMutation()
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Import failed")
            Result.failure(e)
        }
    }

    private suspend fun runMarkdownMergeImport(parsedNotes: List<ParsedMarkdownNote>): MarkdownImportResult {
        var relImported = 0
        var relSkipped = 0
        val linkKeyToNewNoteId = mutableMapOf<String, Long>()

        database.withTransaction {
            for (p in parsedNotes.sortedBy { it.linkKey }) {
                val now = System.currentTimeMillis()
                val created = p.createdAtMillis ?: now
                val updated = p.updatedAtMillis ?: p.createdAtMillis ?: now
                val note = NoteEntity(
                    id = 0,
                    core_id = null,
                    ver = 1,
                    is_deleted = false,
                    title = p.title,
                    text = p.body,
                    recording = null,
                    embedding_vector = null,
                    created_at = created,
                    updated_at = updated,
                    workflow_status = p.status ?: NoteStatus.PUBLISHED
                )
                val newId = noteWriter.insertNote(note)
                linkKeyToNewNoteId[p.linkKey] = newId

                for (catName in p.categories.map { it.trim() }.filter { it.isNotEmpty() }) {
                    var cat = categoryDao.getCategoryByName(catName)
                    if (cat == null) {
                        val catId = categoryDao.insertCategory(
                            CategoryEntity(
                                id = 0,
                                core_id = null,
                                ver = 1,
                                is_deleted = false,
                                name = catName,
                                created_at = now,
                                updated_at = now
                            )
                        )
                        cat = categoryDao.getCategoryById(catId)
                    }
                    if (cat != null) {
                        noteCategoryDao.insertNoteCategory(
                            NoteCategoryCrossRef(
                                note_id = newId,
                                category_id = cat.id,
                                note_core_id = null,
                                category_core_id = null,
                                ver = 1,
                                is_deleted = false,
                                updated_at = now
                            )
                        )
                    }
                }
            }

            for (p in parsedNotes) {
                val currentId = linkKeyToNewNoteId[p.linkKey] ?: continue
                for ((label, targetKey) in p.relationships) {
                    val resolved = resolveRelationshipLabel(label) ?: run {
                        relSkipped++
                        continue
                    }
                    val targetId = linkKeyToNewNoteId[targetKey] ?: run {
                        relSkipped++
                        continue
                    }
                    if (currentId == targetId) {
                        relSkipped++
                        continue
                    }
                    val (noteA, noteB) = if (resolved.type == RelationshipType.FOLLOWS_ON) {
                        if (resolved.noteAIsCurrent) currentId to targetId else targetId to currentId
                    } else {
                        currentId to targetId
                    }
                    if (resolved.type.isSymmetricForImportDedupe()) {
                        val existingFwd = relationshipDao.getRelationshipBetweenNotesWithType(noteA, noteB, resolved.type)
                        val existingRev = relationshipDao.getRelationshipBetweenNotesWithType(noteB, noteA, resolved.type)
                        if (existingFwd != null || existingRev != null) {
                            relSkipped++
                            continue
                        }
                    } else {
                        if (relationshipDao.getRelationshipBetweenNotesWithType(noteA, noteB, resolved.type) != null) {
                            relSkipped++
                            continue
                        }
                    }
                    val now = System.currentTimeMillis()
                    relationshipDao.insertRelationship(
                        RelationshipEntity(
                            id = 0,
                            note_a_core_id = null,
                            note_b_core_id = null,
                            ver = 1,
                            is_deleted = false,
                            note_a_id = noteA,
                            note_b_id = noteB,
                            type = resolved.type,
                            created_at = now,
                            updated_at = now
                        )
                    )
                    relImported++
                }
            }
        }

        return MarkdownImportResult(
            notesImported = parsedNotes.size,
            relationshipsImported = relImported,
            relationshipsSkipped = relSkipped
        )
    }

    private fun loadParsedNotesFromUri(uri: Uri): List<ParsedMarkdownNote>? {
        val stream1 = context.contentResolver.openInputStream(uri) ?: return null
        val allBytes = stream1.use { it.readBytes() }
        if (allBytes.size < 2) return null
        return if (isZipMagic(allBytes)) {
            importFromZipBytes(allBytes)
        } else {
            val text = allBytes.toString(StandardCharsets.UTF_8)
            val linkKey = queryDisplayNameStem(uri) ?: "imported"
            listOf(parseMarkdownNoteFile(linkKey, text))
        }
    }

    private fun importFromZipBytes(bytes: ByteArray): List<ParsedMarkdownNote> {
        val paths = mutableListOf<String>()
        val fileContents = linkedMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                try {
                    paths.add(entry.name)
                    val data = zis.readBytes()
                    if (!entry.isDirectory && zipEntryShouldRead(entry.name)) {
                        fileContents[entry.name.trim().removePrefix("/")] = data
                    }
                } finally {
                    zis.closeEntry()
                }
            }
        }
        if (!validateZipOnlyMarkdownPaths(paths)) {
            throw IllegalArgumentException("Zip must contain only .md files at the archive root")
        }
        return fileContents.entries
            .sortedBy { it.key }
            .map { (name, data) ->
                val linkKey = name.removeSuffix(".md").removeSuffix(".MD")
                parseMarkdownNoteFile(linkKey, data.toString(StandardCharsets.UTF_8))
            }
    }

    private fun queryDisplayNameStem(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val name = it.getString(0) ?: return null
            return name.removeSuffix(".md").removeSuffix(".MD").ifBlank { null }
        }
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun generateZipFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "flit-$timestamp.zip"
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun exportBytesToDownloadsMediaStore(fileName: String, mimeType: String, bytes: ByteArray): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val insertUri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw Exception("Failed to create file in Downloads folder")
        context.contentResolver.openOutputStream(insertUri)?.use { it.write(bytes) }
            ?: throw Exception("Failed to open output stream for file")
        return "Downloads/$fileName"
    }

    private fun exportBytesToDownloadsLegacy(fileName: String, bytes: ByteArray): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }
}

private fun isZipMagic(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
