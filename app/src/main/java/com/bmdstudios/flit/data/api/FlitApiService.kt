package com.bmdstudios.flit.data.api

import com.bmdstudios.flit.data.api.model.ConnectExchangeRequest
import com.bmdstudios.flit.data.api.model.ConnectExchangeResponse
import com.bmdstudios.flit.data.api.model.TokenRequest
import com.bmdstudios.flit.data.api.model.TokenResponse

import com.bmdstudios.flit.data.api.model.CategorySync
import com.bmdstudios.flit.data.api.model.CompareNotesRequest
import com.bmdstudios.flit.data.api.model.NotesCompareResult
import com.bmdstudios.flit.data.api.model.NoteSync
import com.bmdstudios.flit.data.api.model.SyncNotesResponse

import com.bmdstudios.flit.data.api.model.CompareCategoriesRequest
import com.bmdstudios.flit.data.api.model.CategoriesCompareResult
import com.bmdstudios.flit.data.api.model.SyncCategoriesResponse

import com.bmdstudios.flit.data.api.model.CompareNoteCategoriesRequest
import com.bmdstudios.flit.data.api.model.NoteCategoriesCompareResult
import com.bmdstudios.flit.data.api.model.NoteCategorySync
import com.bmdstudios.flit.data.api.model.SyncNoteCategoriesResponse

import com.bmdstudios.flit.data.api.model.CompareRelationshipsRequest
import com.bmdstudios.flit.data.api.model.RelationshipsCompareResult
import com.bmdstudios.flit.data.api.model.RelationshipSync
import com.bmdstudios.flit.data.api.model.SyncRelationshipsResponse

import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.toAppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * Service for communicating with the Flit Core backend API.
 */
class FlitApiService(
    private val client: OkHttpClient,
    private val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Exchanges a connection code for access and refresh tokens.
     *
     * @param request The connection exchange request containing code and device metadata
     * @return Result containing the response with tokens, or an error
     */
    suspend fun exchangeConnectionCode(
        request: ConnectExchangeRequest
    ): Result<ConnectExchangeResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/connect/exchange"
            Timber.d("Exchanging connection code at: $url")

            val requestBody = json.encodeToString(ConnectExchangeRequest.serializer(), request)
                .toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                handleResponse(response)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "exchangeConnectionCode")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Refreshes the access token using the refresh token.
     * Calls POST /oauth/token with grant_type=refresh_token.
     *
     * @param refreshToken The stored refresh token
     * @return Result containing new access token and optional new refresh token, or an error
     */
    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/oauth/token"
            val request = TokenRequest(refreshToken = refreshToken)
            val requestBody = json.encodeToString(TokenRequest.serializer(), request)
                .toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            client.newCall(httpRequest).execute().use { response ->
                handleTokenResponse(response)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "refreshToken")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    private fun handleTokenResponse(response: Response): Result<TokenResponse> {
        return try {
            val bodyString = response.body?.string()
            when {
                !response.isSuccessful -> {
                    val errorMessage = extractErrorMessage(bodyString)
                    val error = AppError.NetworkError.HttpError(
                        statusCode = response.code,
                        serverMessage = errorMessage
                    )
                    Timber.w("Token refresh failed ${response.code}: ${bodyString?.take(200)}")
                    ErrorHandler.logError(error)
                    Result.failure(error.toException())
                }
                bodyString.isNullOrBlank() -> {
                    val error = AppError.NetworkError.DownloadError("Empty response body")
                    ErrorHandler.logError(error)
                    Result.failure(error.toException())
                }
                else -> {
                    val data = json.decodeFromString<TokenResponse>(bodyString)
                    Result.success(data)
                }
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "handleTokenResponse")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Handles HTTP response and converts it to a Result.
     */
    private fun handleResponse(response: Response): Result<ConnectExchangeResponse> {
        return try {
            val bodyString = response.body?.string()
            Timber.d("Response status: ${response.code}, body: ${bodyString?.take(100)}...")

            when {
                !response.isSuccessful -> {
                    // Try to extract error detail from response body
                    val errorMessage = extractErrorMessage(bodyString)
                    val error = AppError.NetworkError.HttpError(
                        statusCode = response.code,
                        serverMessage = errorMessage
                    )
                    Timber.w("HTTP error ${response.code}: ${bodyString?.take(200)}")
                    ErrorHandler.logError(error)
                    Result.failure(error.toException())
                }
                bodyString == null -> {
                    val error = AppError.NetworkError.DownloadError("Empty response body")
                    ErrorHandler.logError(error)
                    Result.failure(error.toException())
                }
                else -> {
                    try {
                        val responseData = json.decodeFromString<ConnectExchangeResponse>(bodyString)
                        Timber.d("Successfully exchanged connection code")
                        Result.success(responseData)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse response JSON")
                        val error = ErrorHandler.transform(e, "parseConnectExchangeResponse")
                        ErrorHandler.logError(error)
                        Result.failure(error.toException())
                    }
                }
            }
        } catch (e: IOException) {
            val error = ErrorHandler.transform(e, "handleResponse")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Extracts error message from API error response body.
     * Backend returns errors in format: {"detail": "error message"}
     */
    private fun extractErrorMessage(bodyString: String?): String? {
        if (bodyString.isNullOrBlank()) return null
        
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(bodyString)
            errorResponse.detail
        } catch (e: Exception) {
            // If parsing fails, return null to use default error message
            Timber.d("Failed to parse error response: ${e.message}")
            null
        }
    }

    /**
     * Data class for parsing error responses from the backend.
     */
    @kotlinx.serialization.Serializable
    private data class ErrorResponse(
        val detail: String
    )

    // ---------- Sync: authenticated request execution ----------

    private suspend fun <T> executeSync(
        token: String,
        request: Request,
        parseBody: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val authed = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(authed).execute().use { response ->
                val bodyString = response.body?.string()
                when {
                    !response.isSuccessful -> {
                        val msg = extractErrorMessage(bodyString)
                        val err = AppError.NetworkError.HttpError(
                            statusCode = response.code,
                            serverMessage = msg
                        )
                        ErrorHandler.logError(err)
                        Result.failure(err.toException())
                    }
                    bodyString == null -> {
                        val err = AppError.NetworkError.DownloadError("Empty response body")
                        ErrorHandler.logError(err)
                        Result.failure(err.toException())
                    }
                    else -> try {
                        Result.success(parseBody(bodyString))
                    } catch (e: Exception) {
                        val err = ErrorHandler.transform(e, "parseSyncResponse")
                        ErrorHandler.logError(err)
                        Result.failure(err.toException())
                    }
                }
            }
        } catch (e: Exception) {
            val err = ErrorHandler.transform(e, "executeSync")
            ErrorHandler.logError(err)
            Result.failure(err.toException())
        }
    }

    // ---------- Compare (POST /sync/compare/*) ----------

    suspend fun compareNotes(token: String, request: CompareNotesRequest): Result<NotesCompareResult> {
        val body = json.encodeToString(CompareNotesRequest.serializer(), request)
            .toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$baseUrl/sync/compare/notes")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeSync(token, req) { json.decodeFromString<NotesCompareResult>(it) }
    }

    suspend fun compareCategories(token: String, request: CompareCategoriesRequest): Result<CategoriesCompareResult> {
        val body = json.encodeToString(CompareCategoriesRequest.serializer(), request)
            .toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$baseUrl/sync/compare/categories")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeSync(token, req) { json.decodeFromString<CategoriesCompareResult>(it) }
    }

    suspend fun compareRelationships(token: String, request: CompareRelationshipsRequest): Result<RelationshipsCompareResult> {
        val body = json.encodeToString(CompareRelationshipsRequest.serializer(), request)
            .toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$baseUrl/sync/compare/relationships")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeSync(token, req) { json.decodeFromString<RelationshipsCompareResult>(it) }
    }

    suspend fun compareNoteCategories(token: String, request: CompareNoteCategoriesRequest): Result<NoteCategoriesCompareResult> {
        val body = json.encodeToString(CompareNoteCategoriesRequest.serializer(), request)
            .toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$baseUrl/sync/compare/note-categories")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeSync(token, req) { json.decodeFromString<NoteCategoriesCompareResult>(it) }
    }

    // ---------- GET sync (single-entity pull by core_id) ----------

    suspend fun getNote(token: String, coreId: Long): Result<SyncNotesResponse> {
        val url = "$baseUrl/sync/notes?core_id=$coreId"
        val req = Request.Builder().url(url).get().build()
        return executeSync(token, req) { json.decodeFromString<SyncNotesResponse>(it) }
    }

    suspend fun getCategory(token: String, coreId: Long): Result<SyncCategoriesResponse> {
        val url = "$baseUrl/sync/categories?core_id=$coreId"
        val req = Request.Builder().url(url).get().build()
        return executeSync(token, req) { json.decodeFromString<SyncCategoriesResponse>(it) }
    }

    suspend fun getRelationship(
        token: String,
        noteACoreId: Long,
        noteBCoreId: Long
    ): Result<SyncRelationshipsResponse> {
        val url = "$baseUrl/sync/relationships?note_a_core_id=$noteACoreId&note_b_core_id=$noteBCoreId"
        val req = Request.Builder().url(url).get().build()
        return executeSync(token, req) { json.decodeFromString<SyncRelationshipsResponse>(it) }
    }

    suspend fun getNoteCategory(
        token: String,
        noteCoreId: Long,
        categoryCoreId: Long
    ): Result<SyncNoteCategoriesResponse> {
        val url = "$baseUrl/sync/note-categories?note_core_id=$noteCoreId&category_core_id=$categoryCoreId"
        val req = Request.Builder().url(url).get().build()
        return executeSync(token, req) { json.decodeFromString<SyncNoteCategoriesResponse>(it) }
    }

    // ---------- POST sync (push, single-entity) ----------

    suspend fun pushNote(token: String, note: NoteSync): Result<com.bmdstudios.flit.data.api.model.SyncPushResult> {
        val body = json.encodeToString(NoteSync.serializer(), note).toRequestBody(jsonMediaType)
        val req = Request.Builder().url("$baseUrl/sync/notes").post(body).addHeader("Content-Type", "application/json").build()
        return executeSync(token, req) { json.decodeFromString<com.bmdstudios.flit.data.api.model.SyncPushResult>(it) }
    }

    suspend fun pushCategory(token: String, category: CategorySync): Result<com.bmdstudios.flit.data.api.model.SyncCategoryPushResult> {
        val body = json.encodeToString(CategorySync.serializer(), category).toRequestBody(jsonMediaType)
        val req = Request.Builder().url("$baseUrl/sync/categories").post(body).addHeader("Content-Type", "application/json").build()
        return executeSync(token, req) { json.decodeFromString<com.bmdstudios.flit.data.api.model.SyncCategoryPushResult>(it) }
    }

    suspend fun pushRelationship(token: String, relationship: RelationshipSync): Result<com.bmdstudios.flit.data.api.model.SyncRelationshipPushResult> {
        val body = json.encodeToString(RelationshipSync.serializer(), relationship).toRequestBody(jsonMediaType)
        val req = Request.Builder().url("$baseUrl/sync/relationships").post(body).addHeader("Content-Type", "application/json").build()
        return executeSync(token, req) { json.decodeFromString<com.bmdstudios.flit.data.api.model.SyncRelationshipPushResult>(it) }
    }

    suspend fun pushNoteCategory(token: String, noteCategory: NoteCategorySync): Result<com.bmdstudios.flit.data.api.model.SyncNoteCategoryPushResult> {
        val body = json.encodeToString(NoteCategorySync.serializer(), noteCategory).toRequestBody(jsonMediaType)
        val req = Request.Builder().url("$baseUrl/sync/note-categories").post(body).addHeader("Content-Type", "application/json").build()
        return executeSync(token, req) { json.decodeFromString<com.bmdstudios.flit.data.api.model.SyncNoteCategoryPushResult>(it) }
    }
}
