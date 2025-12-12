package io.music_assistant.client.ui.compose.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryArgs(
    val name: String,
    val queueOrPlayerId: String
)