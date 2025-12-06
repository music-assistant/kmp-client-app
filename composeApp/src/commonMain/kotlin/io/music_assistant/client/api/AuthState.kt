package io.music_assistant.client.api

/**
 * Represents the authentication state of the connection.
 */
sealed class AuthState {
    /** Not connected yet, no auth state */
    data object NotConnected : AuthState()

    /** Connected but auth not required (schema < 28) */
    data object NotRequired : AuthState()

    /** Connected and auth is required, waiting for credentials */
    data object Required : AuthState()

    /** Authentication is in progress */
    data object Authenticating : AuthState()

    /** Successfully authenticated */
    data class Authenticated(val user: User?) : AuthState()

    /** Authentication failed */
    data class Failed(val reason: String) : AuthState()
}

/**
 * User information returned after successful authentication.
 */
data class User(
    val userId: String,
    val username: String?,
    val displayName: String?,
    val role: String?,
    val avatarUrl: String? = null
)

/**
 * Authentication token information.
 */
data class AuthToken(
    val tokenId: String,
    val name: String,
    val createdAt: Long? = null,
    val lastUsed: Long? = null
)
