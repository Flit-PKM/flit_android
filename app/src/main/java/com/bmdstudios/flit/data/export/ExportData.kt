package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import kotlinx.serialization.Serializable

/**
 * Metadata about the export operation.
 */
@Serializable
data class ExportMetadata(
    val timestamp: Long,
    val app_version: String,
    val database_version: Int
)

/**
 * Simplified note data for export (excluding binary data like embedding vectors).
 */
@Serializable
data class ExportNote(
    val id: Long,
    val title: String,
    val text: String,
    val recording: String? = null,
    val created_at: Long,
    val updated_at: Long,
    val status: String // Serialized as string
)

/**
 * Simplified category data for export.
 */
@Serializable
data class ExportCategory(
    val id: Long,
    val name: String,
    val created_at: Long
)

/**
 * Simplified relationship data for export.
 */
@Serializable
data class ExportRelationship(
    val id: Long,
    val note_a_id: Long,
    val note_b_id: Long,
    val type: String, // Serialized as string
    val created_at: Long
)

/**
 * Note-category cross-reference for export.
 */
@Serializable
data class ExportNoteCategory(
    val note_id: Long,
    val category_id: Long
)

/**
 * Complete export data structure containing all database entities.
 */
@Serializable
data class ExportData(
    val export_metadata: ExportMetadata,
    val notes: List<ExportNote>,
    val categories: List<ExportCategory>,
    val relationships: List<ExportRelationship>,
    val note_categories: List<ExportNoteCategory>
)

/**
 * Extension functions to convert entities to export format.
 */
fun NoteEntity.toExportNote(): ExportNote {
    return ExportNote(
        id = id,
        title = title,
        text = text,
        recording = recording,
        created_at = created_at,
        updated_at = updated_at,
        status = workflow_status.name
    )
}

fun CategoryEntity.toExportCategory(): ExportCategory {
    return ExportCategory(
        id = id,
        name = name,
        created_at = created_at
    )
}

fun RelationshipEntity.toExportRelationship(): ExportRelationship {
    return ExportRelationship(
        id = id,
        note_a_id = note_a_id,
        note_b_id = note_b_id,
        type = type.name,
        created_at = created_at
    )
}

fun NoteCategoryCrossRef.toExportNoteCategory(): ExportNoteCategory {
    return ExportNoteCategory(
        note_id = note_id,
        category_id = category_id
    )
}
