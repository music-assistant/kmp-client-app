package io.music_assistant.client.data.model.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("access_token") val token: String? = null,
    @SerialName("user") val user: User? = null,
    @SerialName("error") val error: String? = null,
)

@Serializable
data class User(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("role") val role: String? = null,
) {
    val description: String
        get() = (displayName ?: username ?: "Unknown User") + if (role != null) " ($role)" else ""
}

@Serializable
data class AuthorizationResponse(
    @SerialName("user") val user: User,
)