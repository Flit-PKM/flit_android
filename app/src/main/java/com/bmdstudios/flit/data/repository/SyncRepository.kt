package com.bmdstudios.flit.data.repository

import com.bmdstudios.flit.data.api.FlitApiService
import com.bmdstudios.flit.data.api.model.CategorySync
import com.bmdstudios.flit.data.api.model.CategoryVersion
import com.bmdstudios.flit.data.api.model.NoteCategorySync
import com.bmdstudios.flit.data.api.model.NoteCategoryVersion
import com.bmdstudios.flit.data.api.model.NoteSync
import com.bmdstudios.flit.data.api.model.NoteType
import com.bmdstudios.flit.data.api.model.NoteVersion
import com.bmdstudios.flit.data.api.model.RelationshipSync
import com.bmdstudios.flit.data.api.model.RelationshipTypeApi
import com.bmdstudios.flit.data.api.model.RelationshipVersion
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.database.model.RelationshipType
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.NoteWriter
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.AppErrorException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val SYNC_TAG = "Sync"

/**
 * Orchestrates sync with the Flit Core backend.
 * Runs one table at a time: for each table, compare then all pull/push for that table.
 * Tables run in order (notes → categories → relationships → note-categories)
 * to avoid race conditions on composite ids. Within a table, pull and push requests
 * complete before moving to the next table.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val api: FlitApiService,
    private val settings: SettingsRepository,
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val relationshipDao: RelationshipDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val noteWriter: NoteWriter
) {

    sealed class SyncResult {
        data object NotAuthenticated : SyncResult()
        data class Error(val message: String) : SyncResult()
        data object Success : SyncResult()
    }

    suspend fun runSync(): SyncResult = withContext(Dispatchers.IO) {
        val token = settings.getAccessToken()
        if (token == null) {
            Timber.tag(SYNC_TAG).w("Sync skipped: no access token")
            return@withContext SyncResult.NotAuthenticated
        }
        Timber.tag(SYNC_TAG).d("Sync started")

        val firstAttempt = kotlin.runCatching { doSync(token) }
        firstAttempt.fold(
            onSuccess = {
                Timber.tag(SYNC_TAG).d("Sync completed successfully")
                return@withContext SyncResult.Success
            },
            onFailure = { e ->
                if (!isTokenExpiredError(e)) {
                    Timber.tag(SYNC_TAG).e(e, "Sync failed")
                    return@withContext SyncResult.Error(e.message ?: "Unknown error")
                }
                Timber.tag(SYNC_TAG).w("Sync failed with token expiry, attempting refresh")
                val refreshToken = settings.getRefreshToken()
                if (refreshToken == null) {
                    Timber.tag(SYNC_TAG).w("No refresh token available")
                    settings.clearTokens()
                    return@withContext SyncResult.NotAuthenticated
                }
                val refreshResult = api.refreshToken(refreshToken)
                refreshResult.fold(
                    onSuccess = { tr ->
                        val newRefresh = tr.refreshToken ?: refreshToken
                        settings.saveTokens(tr.accessToken, newRefresh, tr.expiresIn)
                        Timber.tag(SYNC_TAG).d("Token refreshed successfully")
                        val retryResult = kotlin.runCatching { doSync(tr.accessToken) }.fold(
                            onSuccess = { SyncResult.Success },
                            onFailure = { retryEx ->
                                Timber.tag(SYNC_TAG).e(retryEx, "Sync failed after token refresh")
                                SyncResult.Error(retryEx.message ?: "Unknown error")
                            }
                        )
                        return@withContext retryResult
                    },
                    onFailure = {
                        Timber.tag(SYNC_TAG).w(it, "Token refresh failed")
                        settings.clearTokens()
                        return@withContext SyncResult.NotAuthenticated
                    }
                )
            }
        )
    }

    private fun isTokenExpiredError(e: Throwable): Boolean {
        val err = (e as? AppErrorException)?.appError as? AppError.NetworkError.HttpError ?: return false
        return err.statusCode == 401 &&
            (err.serverMessage?.contains("Invalid or expired token", ignoreCase = true) == true)
    }

    /**
     * Performs sync using the given access token.
     * Each table is fully synced (compare then all pull/push) before the next table starts.
     * Throws on any API or apply failure.
     */
    private suspend fun doSync(token: String) {
        // Notes: compare then pull/push
        val notesCompare = api.compareNotes(token, buildNotesCompareRequest()).getOrThrow()
        Timber.tag(SYNC_TAG).d("Notes compare: toPull=${notesCompare.toPull.size}, toPush=${notesCompare.toPush.size}")
        syncNotesTable(token, notesCompare)

        // Categories: compare then pull/push
        val categoriesCompare = api.compareCategories(token, buildCategoriesCompareRequest()).getOrThrow()
        Timber.tag(SYNC_TAG).d("Categories compare: toPull=${categoriesCompare.toPull.size}, toPush=${categoriesCompare.toPush.size}")
        syncCategoriesTable(token, categoriesCompare)

        // Relationships: compare then pull/push
        val relationshipsCompare = api.compareRelationships(token, buildRelationshipsCompareRequest()).getOrThrow()
        Timber.tag(SYNC_TAG).d("Relationships compare: toPull=${relationshipsCompare.toPull.size}, toPush=${relationshipsCompare.toPush.size}")
        syncRelationshipsTable(token, relationshipsCompare)

        // Note-categories: compare then pull/push
        val noteCategoriesCompare = api.compareNoteCategories(token, buildNoteCategoriesCompareRequest()).getOrThrow()
        Timber.tag(SYNC_TAG).d("Note-categories compare: toPull=${noteCategoriesCompare.toPull.size}, toPush=${noteCategoriesCompare.toPush.size}")
        syncNoteCategoriesTable(token, noteCategoriesCompare)
    }

    // -------- Compare request builders --------

    private suspend fun buildNotesCompareRequest() =
        com.bmdstudios.flit.data.api.model.CompareNotesRequest(
            notes = noteDao.getAllNotesForSync().map { n ->
                NoteVersion(
                    coreId = n.core_id,
                    appId = n.id,
                    version = n.ver,
                    isDeleted = n.is_deleted
                )
            }
        )

    private suspend fun buildCategoriesCompareRequest() =
        com.bmdstudios.flit.data.api.model.CompareCategoriesRequest(
            categories = categoryDao.getAllCategoriesForSync().map { c ->
                CategoryVersion(
                    coreId = c.core_id,
                    appId = c.id,
                    version = c.ver,
                    isDeleted = c.is_deleted
                )
            }
        )

    private suspend fun buildRelationshipsCompareRequest(): com.bmdstudios.flit.data.api.model.CompareRelationshipsRequest {
        val all = relationshipDao.getAllRelationshipsForSync()
        val noteCoreByLocalId = noteDao.getAllNotesForSync().associate { it.id to it.core_id }
        val items = all.mapNotNull { r ->
            val coreA = r.note_a_core_id ?: noteCoreByLocalId[r.note_a_id]
            val coreB = r.note_b_core_id ?: noteCoreByLocalId[r.note_b_id]
            if (coreA != null && coreB != null) {
                RelationshipVersion(noteAId = coreA, noteBId = coreB, version = r.ver, isDeleted = r.is_deleted)
            } else null
        }
        val skipped = all.size - items.size
        if (skipped > 0) {
            Timber.tag(SYNC_TAG).d("Relationships compare: included=${items.size}, skipped=$skipped (null core IDs)")
        }
        return com.bmdstudios.flit.data.api.model.CompareRelationshipsRequest(relationships = items)
    }

    private suspend fun buildNoteCategoriesCompareRequest(): com.bmdstudios.flit.data.api.model.CompareNoteCategoriesRequest {
        val links = noteCategoryDao.getAllNoteCategoriesForSync()
        val noteCoreById = noteDao.getAllNotesForSync().filter { it.core_id != null }.associate { it.id to it.core_id!! }
        val categoryCoreById = categoryDao.getAllCategoriesForSync().filter { it.core_id != null }.associate { it.id to it.core_id!! }
        val items = links.mapNotNull { nc ->
            val noteCore = noteCoreById[nc.note_id] ?: return@mapNotNull null
            val catCore = categoryCoreById[nc.category_id] ?: return@mapNotNull null
            NoteCategoryVersion(
                noteId = noteCore,
                categoryId = catCore,
                version = nc.ver,
                isDeleted = nc.is_deleted
            )
        }
        return com.bmdstudios.flit.data.api.model.CompareNoteCategoriesRequest(noteCategories = items)
    }

    // -------- Per-table sync (GET then apply, POST then apply) --------

    private suspend fun syncNotesTable(token: String, result: com.bmdstudios.flit.data.api.model.NotesCompareResult) {
        val pullIds = result.toPull.mapNotNull { it.coreId }
        Timber.tag(SYNC_TAG).d("Notes: pulling ${pullIds.size}")
        val collected = mutableListOf<com.bmdstudios.flit.data.api.model.SyncNoteItem>()
        for (coreId in pullIds) {
            api.getNote(token, coreId).fold(
                onSuccess = { collected.add(it.note) },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "GET /sync/notes core_id=$coreId failed, skipping") }
            )
        }
        if (collected.isNotEmpty()) applyNotesGet(collected)
        val allNotes = noteDao.getAllNotesForSync()
        val noteByAppId = allNotes.associateBy { it.id }
        val noteByCoreId = allNotes.filter { it.core_id != null }.associateBy { it.core_id!! }
        val toPush = result.toPush.mapNotNull { v ->
            val local = v.appId?.let { id -> noteByAppId[id] } ?: v.coreId?.let { noteByCoreId[it] }
            local?.let {
                val dto = entityToNoteSync(it).copy(coreId = v.coreId ?: it.core_id)
                dto to it
            }
        }
        Timber.tag(SYNC_TAG).d("Notes: pushing ${toPush.size}")
        for ((dto, local) in toPush) {
            api.pushNote(token, dto).fold(
                onSuccess = { pushResult ->
                    if (pushResult.status != "rejected" && pushResult.serverVersion != null) {
                        val updated = local.copy(core_id = pushResult.coreId, ver = pushResult.serverVersion)
                        noteWriter.updateNote(updated)
                    } else if (pushResult.status == "rejected") {
                        Timber.tag(SYNC_TAG).w("Notes: push rejected for app_id=${local.id}")
                    }
                },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "Notes: push failed for app_id=${local.id}") }
            )
        }
    }

    private suspend fun syncCategoriesTable(token: String, result: com.bmdstudios.flit.data.api.model.CategoriesCompareResult) {
        val pullIds = result.toPull.mapNotNull { it.coreId }
        Timber.tag(SYNC_TAG).d("Categories: pulling ${pullIds.size}")
        val collected = mutableListOf<com.bmdstudios.flit.data.api.model.SyncCategoryItem>()
        for (coreId in pullIds) {
            api.getCategory(token, coreId).fold(
                onSuccess = { collected.add(it.category) },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "GET /sync/categories core_id=$coreId failed, skipping") }
            )
        }
        if (collected.isNotEmpty()) applyCategoriesGet(collected)
        val allCategories = categoryDao.getAllCategoriesForSync()
        val categoryByAppId = allCategories.associateBy { it.id }
        val categoryByCoreId = allCategories.filter { it.core_id != null }.associateBy { it.core_id!! }
        val toPush = result.toPush.mapNotNull { v ->
            val local = v.appId?.let { categoryByAppId[it] } ?: v.coreId?.let { categoryByCoreId[it] }
            local?.let {
                val dto = entityToCategorySync(it).copy(coreId = v.coreId ?: it.core_id)
                dto to it
            }
        }
        Timber.tag(SYNC_TAG).d("Categories: pushing ${toPush.size}")
        for ((dto, local) in toPush) {
            api.pushCategory(token, dto).fold(
                onSuccess = { pushResult ->
                    if (pushResult.status != "rejected" && pushResult.serverVersion != null) {
                        categoryDao.updateCategory(local.copy(core_id = pushResult.coreId, ver = pushResult.serverVersion))
                    } else if (pushResult.status == "rejected") {
                        Timber.tag(SYNC_TAG).w("Categories: push rejected for app_id=${local.id}")
                    }
                },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "Categories: push failed for app_id=${local.id}") }
            )
        }
    }

    private suspend fun syncRelationshipsTable(token: String, result: com.bmdstudios.flit.data.api.model.RelationshipsCompareResult) {
        val pullPairs = result.toPull
        Timber.tag(SYNC_TAG).d("Relationships: pulling ${pullPairs.size}")
        val collected = mutableListOf<com.bmdstudios.flit.data.api.model.SyncRelationshipItem>()
        for (p in pullPairs) {
            api.getRelationship(token, p.noteAId, p.noteBId).fold(
                onSuccess = { collected.add(it.relationship) },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "GET /sync/relationships ${p.noteAId},${p.noteBId} failed, skipping") }
            )
        }
        if (collected.isNotEmpty()) applyRelationshipsGet(collected)
        val noteCoreToLocal = noteDao.getAllNotesForSync()
            .filter { it.core_id != null }
            .associate { it.core_id!! to it.id }
        val toPush = result.toPush.mapNotNull { v ->
            val byCore = relationshipDao.getRelationshipByCorePair(v.noteAId, v.noteBId)
            val entity = if (byCore != null) {
                Timber.tag(SYNC_TAG).d("Relationships: push entity id=${byCore.id} looked up by core pair")
                byCore
            } else {
                val localA = noteCoreToLocal[v.noteAId] ?: return@mapNotNull null
                val localB = noteCoreToLocal[v.noteBId] ?: return@mapNotNull null
                relationshipDao.getRelationshipByLocalNotePairForSync(localA, localB)?.also {
                    Timber.tag(SYNC_TAG).d("Relationships: push entity id=${it.id} looked up by local pair")
                } ?: return@mapNotNull null
            }
            entityToRelationshipSync(entity, noteACoreId = v.noteAId, noteBCoreId = v.noteBId) to entity
        }
        Timber.tag(SYNC_TAG).d("Relationships: pushing ${toPush.size}")
        for ((dto, local) in toPush) {
            api.pushRelationship(token, dto).fold(
                onSuccess = { pushResult ->
                    if (pushResult.status != "rejected" && pushResult.serverVersion != null) {
                        relationshipDao.updateRelationship(
                            local.copy(note_a_core_id = pushResult.noteACoreId, note_b_core_id = pushResult.noteBCoreId, ver = pushResult.serverVersion)
                        )
                        Timber.tag(SYNC_TAG).d("Relationships: push entity id=${local.id} status=${pushResult.status}")
                    } else if (pushResult.status == "rejected") {
                        Timber.tag(SYNC_TAG).w("Relationships: push rejected for entity id=${local.id}")
                    }
                },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "Relationships: push failed for entity id=${local.id}") }
            )
        }
    }

    private suspend fun syncNoteCategoriesTable(token: String, result: com.bmdstudios.flit.data.api.model.NoteCategoriesCompareResult) {
        val pullPairs = result.toPull
        Timber.tag(SYNC_TAG).d("Note-categories: pulling ${pullPairs.size}")
        val collected = mutableListOf<com.bmdstudios.flit.data.api.model.SyncNoteCategoryItem>()
        for (p in pullPairs) {
            api.getNoteCategory(token, p.noteId, p.categoryId).fold(
                onSuccess = { collected.add(it.noteCategory) },
                onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "GET /sync/note-categories ${p.noteId},${p.categoryId} failed, skipping") }
            )
        }
        if (collected.isNotEmpty()) applyNoteCategoriesGet(collected)
        val pushCount = result.toPush.sumOf { buildNoteCategorySyncItems(it.noteId, it.categoryId).size }
        Timber.tag(SYNC_TAG).d("Note-categories: pushing $pushCount")
        for (v in result.toPush) {
            val items = buildNoteCategorySyncItems(v.noteId, v.categoryId)
            for (dto in items) {
                api.pushNoteCategory(token, dto).fold(
                    onSuccess = { pushResult ->
                        if (pushResult.status != "rejected" && pushResult.serverVersion != null) {
                            val localNote = noteDao.getNoteByCoreId(pushResult.noteCoreId) ?: return@fold
                            val localCat = categoryDao.getCategoryByCoreId(pushResult.categoryCoreId) ?: return@fold
                            val link = noteCategoryDao.getAllNoteCategoriesForSync()
                                .find { it.note_id == localNote.id && it.category_id == localCat.id } ?: return@fold
                            val updated = link.copy(
                                note_core_id = pushResult.noteCoreId,
                                category_core_id = pushResult.categoryCoreId,
                                ver = pushResult.serverVersion,
                                updated_at = System.currentTimeMillis()
                            )
                            noteCategoryDao.updateNoteCategory(updated)
                        } else if (pushResult.status == "rejected") {
                            Timber.tag(SYNC_TAG).w("Note-categories: push rejected for note=${v.noteId}, category=${v.categoryId}")
                        }
                    },
                    onFailure = { e -> Timber.tag(SYNC_TAG).w(e, "Note-categories: push failed for note=${v.noteId}, category=${v.categoryId}") }
                )
            }
        }
    }

    private suspend fun buildNoteCategorySyncItems(noteCoreId: Long, categoryCoreId: Long): List<NoteCategorySync> {
        val note = noteDao.getNoteByCoreId(noteCoreId) ?: return emptyList()
        val category = categoryDao.getCategoryByCoreId(categoryCoreId) ?: return emptyList()
        val link = noteCategoryDao.getAllNoteCategoriesForSync().find { it.note_id == note.id && it.category_id == category.id } ?: return emptyList()
        return listOf(
            NoteCategorySync(
                noteId = noteCoreId,
                categoryId = categoryCoreId,
                version = link.ver,
                isDeleted = link.is_deleted
            )
        )
    }

    // -------- Entity ↔ Sync DTO mapping --------

    private fun entityToNoteSync(n: NoteEntity) = NoteSync(
        coreId = n.core_id,
        title = n.title,
        content = n.text,
        type = when (n.workflow_status) {
            NoteStatus.PUBLISHED -> NoteType.BASE
            NoteStatus.DRAFT, NoteStatus.PROCESSING -> NoteType.BASE
        },
        version = n.ver.coerceAtLeast(1),
        isDeleted = n.is_deleted
    )

    private fun entityToCategorySync(c: CategoryEntity) = CategorySync(
        coreId = c.core_id,
        name = c.name,
        version = c.ver.coerceAtLeast(1),
        isDeleted = c.is_deleted
    )

    private fun entityToRelationshipSync(
        r: RelationshipEntity,
        noteACoreId: Long? = null,
        noteBCoreId: Long? = null
    ) = RelationshipSync(
        noteAId = noteACoreId ?: r.note_a_core_id ?: 0L,
        noteBId = noteBCoreId ?: r.note_b_core_id ?: 0L,
        type = when (r.type) {
            RelationshipType.FOLLOWS_ON -> RelationshipTypeApi.FOLLOWS_ON
            RelationshipType.SIMILAR_TO -> RelationshipTypeApi.SIMILAR_TO
            RelationshipType.CONTRADICTS -> RelationshipTypeApi.CONTRADICTS
            RelationshipType.REFERENCES -> RelationshipTypeApi.REFERENCES
            RelationshipType.RELATED_TO -> RelationshipTypeApi.RELATED_TO
        },
        version = r.ver.coerceAtLeast(1),
        isDeleted = r.is_deleted
    )

    // -------- Apply GET (pull) results to DB --------

    private suspend fun applyNotesGet(notes: List<com.bmdstudios.flit.data.api.model.SyncNoteItem>) {
        val existingByCoreId = noteDao.getNotesByCoreIds(notes.map { it.id }).associateBy { it.core_id!! }
        for (n in notes) {
            val existing = existingByCoreId[n.id]
            val status = NoteStatus.PUBLISHED
            val entity = NoteEntity(
                id = existing?.id ?: 0L,
                core_id = n.id,
                ver = n.version,
                is_deleted = n.isDeleted,
                title = n.title,
                text = n.content,
                recording = null,
                embedding_vector = null,
                created_at = existing?.created_at ?: System.currentTimeMillis(),
                updated_at = System.currentTimeMillis(),
                workflow_status = status
            )
            if (existing != null) {
                noteWriter.updateNote(entity)
            } else {
                noteWriter.insertNote(entity)
            }
        }
    }

    private suspend fun applyCategoriesGet(categories: List<com.bmdstudios.flit.data.api.model.SyncCategoryItem>) {
        val existingByCoreId = categoryDao.getCategoriesByCoreIds(categories.map { it.id }).associateBy { it.core_id!! }
        for (c in categories) {
            val existing = existingByCoreId[c.id]
            val entity = CategoryEntity(
                id = existing?.id ?: 0L,
                core_id = c.id,
                ver = c.version,
                is_deleted = c.isDeleted,
                name = c.name,
                created_at = existing?.created_at ?: System.currentTimeMillis(),
                updated_at = System.currentTimeMillis()
            )
            if (existing != null) categoryDao.updateCategory(entity) else categoryDao.insertCategory(entity)
        }
    }

    private suspend fun applyRelationshipsGet(relationships: List<com.bmdstudios.flit.data.api.model.SyncRelationshipItem>) {
        val noteCoreToLocal = noteDao.getAllNotesForSync().filter { it.core_id != null }.associate { it.core_id!! to it.id }
        val existing = relationshipDao.getAllRelationshipsForSync().filter { it.note_a_core_id != null && it.note_b_core_id != null }
            .associateBy { Pair(it.note_a_core_id!!, it.note_b_core_id!!) }
        for (r in relationships) {
            val localA = noteCoreToLocal[r.noteAId] ?: continue
            val localB = noteCoreToLocal[r.noteBId] ?: continue
            val key = r.noteAId to r.noteBId
            val rel = existing[key]
            val type = RelationshipType.valueOf(r.type)
            val entity = RelationshipEntity(
                id = rel?.id ?: 0L,
                note_a_core_id = r.noteAId,
                note_b_core_id = r.noteBId,
                ver = r.version,
                is_deleted = r.isDeleted,
                note_a_id = localA,
                note_b_id = localB,
                type = type,
                created_at = rel?.created_at ?: System.currentTimeMillis(),
                updated_at = System.currentTimeMillis()
            )
            if (rel != null) relationshipDao.updateRelationship(entity) else relationshipDao.insertRelationship(entity)
        }
    }

    private suspend fun applyNoteCategoriesGet(items: List<com.bmdstudios.flit.data.api.model.SyncNoteCategoryItem>) {
        val noteCoreToLocal = noteDao.getAllNotesForSync().filter { it.core_id != null }.associate { it.core_id!! to it.id }
        val categoryCoreToLocal = categoryDao.getAllCategoriesForSync().filter { it.core_id != null }.associate { it.core_id!! to it.id }
        for (nc in items) {
            val localNote = noteCoreToLocal[nc.noteId] ?: continue
            val localCat = categoryCoreToLocal[nc.categoryId] ?: continue
            val x = NoteCategoryCrossRef(
                note_id = localNote,
                category_id = localCat,
                note_core_id = nc.noteId,
                category_core_id = nc.categoryId,
                ver = nc.version,
                is_deleted = nc.isDeleted,
                updated_at = System.currentTimeMillis()
            )
            noteCategoryDao.insertNoteCategories(listOf(x))
        }
    }

}
