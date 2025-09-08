package io.music_assistant.client.ui.compose.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoutes {
    @Serializable
    data object Main : AppRoutes

    @Serializable
    data class LibraryArgs(
        val name: String,
        val queueOrPlayerId: String
    ) : AppRoutes

    @Serializable
    data object Settings : AppRoutes
}