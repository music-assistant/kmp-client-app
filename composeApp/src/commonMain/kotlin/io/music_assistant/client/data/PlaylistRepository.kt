package io.music_assistant.client.data

import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.utils.resultAs

class PlaylistRepository(
    private val apiClient: ServiceClient
) {
    suspend fun getEditablePlaylists(): List<AppMediaItem.Playlist> {
        val result = apiClient.sendRequest(Request.Playlist.listLibrary())
        return result.resultAs<List<ServerMediaItem>>()
            ?.toAppMediaItemList()
            ?.filterIsInstance<AppMediaItem.Playlist>()
            ?.filter { it.isEditable == true }
            ?: emptyList()
    }

    suspend fun addToPlaylist(
        mediaItem: AppMediaItem,
        playlist: AppMediaItem.Playlist
    ): Result<String> {
        val itemUri = mediaItem.uri ?: return Result.failure(Exception("Media item has no URI"))

        return apiClient.sendRequest(
            Request.Playlist.addTracks(
                playlistId = playlist.itemId,
                trackUris = listOf(itemUri),
            )
        ).map { "Added to ${playlist.name}" }
    }

    suspend fun removeFromPlaylist(
        playlistId: String,
        position: Int
    ): Result<Unit> {
        return apiClient.sendRequest(
            Request.Playlist.removeTracks(
                playlistId = playlistId,
                positions = listOf(position + 1) // +1 because server uses 1-based indexing
            )
        ).map { }
    }
}
