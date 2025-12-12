package io.music_assistant.client.data.model.client

import io.music_assistant.client.ui.compose.library.LibraryArgs

data class PlayerData(
    val player: Player,
    val queue: Queue? = null
) {
    val libraryArgs = LibraryArgs(
        name = player.name,
        queueOrPlayerId = queue?.id ?: player.id
    )
}