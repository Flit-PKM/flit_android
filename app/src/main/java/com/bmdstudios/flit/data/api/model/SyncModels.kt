package com.bmdstudios.flit.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Compare: Version types (core_id, app_id, version, is_deleted) ----------

@Serializable
data class NoteVersion(
    @SerialName("core_id") val coreId: Long? = null,
    @SerialName("app_id") val appId: Long? = null,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class ChunkVersion(
    @SerialName("core_id") val coreId: Long? = null,
    @SerialName("app_id") val appId: Long? = null,
    @SerialName("note_core_id") val noteCoreId: Long? = null,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class CategoryVersion(
    @SerialName("core_id") val coreId: Long? = null,
    @SerialName("app_id") val appId: Long? = null,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

/** (note_a_core_id, note_b_core_id) are server note core ids; backend uses note_a_core_id/note_b_core_id. */
@Serializable
data class RelationshipVersion(
    @SerialName("note_a_core_id") val noteAId: Long,
    @SerialName("note_b_core_id") val noteBId: Long,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

/** (note_core_id, category_core_id) are server core ids; matches backend OpenAPI. */
@Serializable
data class NoteCategoryVersion(
    @SerialName("note_core_id") val noteId: Long,
    @SerialName("category_core_id") val categoryId: Long,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

// ---------- Compare requests ----------

@Serializable
data class CompareNotesRequest(val notes: List<NoteVersion> = emptyList())

@Serializable
data class CompareChunksRequest(val chunks: List<ChunkVersion> = emptyList())

@Serializable
data class CompareCategoriesRequest(val categories: List<CategoryVersion> = emptyList())

@Serializable
data class CompareRelationshipsRequest(val relationships: List<RelationshipVersion> = emptyList())

@Serializable
data class CompareNoteCategoriesRequest(
    @SerialName("note_categories") val noteCategories: List<NoteCategoryVersion> = emptyList()
)

// ---------- Compare results ----------

@Serializable
data class NotesCompareResult(
    @SerialName("to_pull") val toPull: List<NoteVersion> = emptyList(),
    @SerialName("to_push") val toPush: List<NoteVersion> = emptyList()
)

@Serializable
data class ChunksCompareResult(
    @SerialName("to_pull") val toPull: List<ChunkVersion> = emptyList(),
    @SerialName("to_push") val toPush: List<ChunkVersion> = emptyList()
)

@Serializable
data class CategoriesCompareResult(
    @SerialName("to_pull") val toPull: List<CategoryVersion> = emptyList(),
    @SerialName("to_push") val toPush: List<CategoryVersion> = emptyList()
)

@Serializable
data class RelationshipsCompareResult(
    @SerialName("to_pull") val toPull: List<RelationshipVersion> = emptyList(),
    @SerialName("to_push") val toPush: List<RelationshipVersion> = emptyList()
)

@Serializable
data class NoteCategoriesCompareResult(
    @SerialName("to_pull") val toPull: List<NoteCategoryVersion> = emptyList(),
    @SerialName("to_push") val toPush: List<NoteCategoryVersion> = emptyList()
)

// ---------- Sync push DTOs (for POST) ----------

@Serializable
enum class NoteType {
    @SerialName("BASE") BASE,
    @SerialName("INSIGHT") INSIGHT,
    @SerialName("SUMMARY") SUMMARY
}

@Serializable
data class NoteSync(
    @SerialName("core_id") val coreId: Long? = null,
    @SerialName("app_id") val appId: Long? = null,
    val title: String,
    val content: String,
    val type: NoteType = NoteType.BASE,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class ChunkSync(
    @SerialName("core_id") val coreId: Long? = null,
    @SerialName("note_id") val noteId: Long,
    @SerialName("position_start") val positionStart: Int,
    @SerialName("position_end") val positionEnd: Int,
    val summary: String,
    val embedding: List<Float>? = null,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class CategorySync(
    @SerialName("core_id") val coreId: Long? = null,
    val name: String,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
enum class RelationshipTypeApi {
    @SerialName("FOLLOWS_ON") FOLLOWS_ON,
    @SerialName("SIMILAR_TO") SIMILAR_TO,
    @SerialName("CONTRADICTS") CONTRADICTS,
    @SerialName("REFERENCES") REFERENCES,
    @SerialName("RELATED_TO") RELATED_TO
}

@Serializable
data class RelationshipSync(
    @SerialName("note_a_core_id") val noteAId: Long,
    @SerialName("note_b_core_id") val noteBId: Long,
    val type: RelationshipTypeApi,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class NoteCategorySync(
    @SerialName("note_core_id") val noteId: Long,
    @SerialName("category_core_id") val categoryId: Long,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

// ---------- Push response types (server returns these after POST) ----------

@Serializable
data class SyncPushResult(
    @SerialName("core_id") val coreId: Long,
    val status: String,
    @SerialName("server_version") val serverVersion: Int? = null
)

@Serializable
data class SyncChunkPushResult(
    @SerialName("core_id") val coreId: Long,
    val status: String,
    @SerialName("server_version") val serverVersion: Int? = null
)

@Serializable
data class SyncCategoryPushResult(
    @SerialName("core_id") val coreId: Long,
    val status: String,
    @SerialName("server_version") val serverVersion: Int? = null
)

@Serializable
data class SyncRelationshipPushResult(
    @SerialName("note_a_core_id") val noteACoreId: Long,
    @SerialName("note_b_core_id") val noteBCoreId: Long,
    val status: String,
    @SerialName("server_version") val serverVersion: Int? = null
)

@Serializable
data class SyncNoteCategoryPushResult(
    @SerialName("note_core_id") val noteCoreId: Long,
    @SerialName("category_core_id") val categoryCoreId: Long,
    val status: String,
    @SerialName("server_version") val serverVersion: Int? = null
)

// ---------- GET sync response types (parsed from backend "additionalProperties" objects) ----------

/** Minimal structure for note from GET /sync/notes; backend returns NoteRead-shaped objects. */
@Serializable
data class SyncNoteItem(
    @SerialName("core_id") val id: Long,
    val title: String,
    val content: String,
    val type: String = "BASE",
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncNotesResponse(val note: SyncNoteItem)

@Serializable
data class SyncCategoryItem(
    @SerialName("core_id") val id: Long,
    val name: String,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncCategoriesResponse(val category: SyncCategoryItem)

@Serializable
data class SyncChunkItem(
    @SerialName("core_id") val id: Long,
    @SerialName("note_core_id") val noteId: Long,
    @SerialName("position_start") val positionStart: Int,
    @SerialName("position_end") val positionEnd: Int,
    val summary: String,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SyncChunksResponse(val chunk: SyncChunkItem)

@Serializable
data class SyncRelationshipItem(
    @SerialName("note_a_core_id") val noteAId: Long,
    @SerialName("note_b_core_id") val noteBId: Long,
    val type: String,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SyncRelationshipsResponse(val relationship: SyncRelationshipItem)

@Serializable
data class SyncNoteCategoryItem(
    @SerialName("note_core_id") val noteId: Long,
    @SerialName("category_core_id") val categoryId: Long,
    val version: Int,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SyncNoteCategoriesResponse(
    @SerialName("note_category") val noteCategory: SyncNoteCategoryItem
)
