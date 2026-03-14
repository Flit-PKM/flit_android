package com.bmdstudios.flit.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for exchanging connection code for tokens.
 */
@Serializable
data class ConnectExchangeRequest(
    @SerialName("connection_code")
    val connectionCode: String,
    @SerialName("app_slug")
    val appSlug: String,
    @SerialName("device_name")
    val deviceName: String,
    val platform: String,
    @SerialName("app_version")
    val appVersion: String
)

/**
 * Response model containing access and refresh tokens.
 */
@Serializable
data class ConnectExchangeResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "Bearer",
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String
)

/**
 * Request model for OAuth2 token refresh (/oauth/token).
 */
@Serializable
data class TokenRequest(
    @SerialName("grant_type")
    val grantType: String = "refresh_token",
    @SerialName("refresh_token")
    val refreshToken: String
)

/**
 * Response model for OAuth2 token endpoint.
 * refresh_token is optional; backend may omit it when using rotating refresh tokens.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "Bearer",
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String? = null
)
