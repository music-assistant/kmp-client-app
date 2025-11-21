package io.music_assistant.client.data.model.client

import io.music_assistant.client.ui.compose.nav.AppRoutes

data class PlayerData(
    val player: Player,
    val queue: Queue? = null,
) {
    val libraryArgs =
        AppRoutes.LibraryArgs(
            name = player.name,
            queueOrPlayerId = queue?.id ?: player.id,
        )
}
