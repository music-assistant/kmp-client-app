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

    suspend fun addTrackToPlaylist(
        track: AppMediaItem.Track,
        playlist: AppMediaItem.Playlist
    ): Result<String> {
        val trackUri = track.uri ?: return Result.failure(Exception("Track has no URI"))

        return apiClient.sendRequest(
            Request.Playlist.addTracks(
                playlistId = playlist.itemId,
                trackUris = listOf(trackUri),
            )
        ).map { "Added to ${playlist.name}" }
    }
}
