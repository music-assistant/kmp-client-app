package io.music_assistant.client.ui.compose.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.utils.resultAs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * VM that provides library and favorites actions for media items.
 * Can be used by any view that needs to add/remove items from library or favorites.
 */
class ActionsViewModel(
    private val apiClient: ServiceClient
) : ViewModel() {

    private val _toasts = MutableSharedFlow<String>()
    val toasts = _toasts.asSharedFlow()

    /**
     * Toggles library status of the item.
     * Adds to library if not in library, removes if already in library.
     */
    fun onLibraryClick(item: AppMediaItem) {
        viewModelScope.launch {
            if (item.isInLibrary) {
                apiClient.sendRequest(
                    Request.Library.remove(item.itemId, item.mediaType)
                )
            } else {
                item.uri?.let {
                    apiClient.sendRequest(Request.Library.add(item.uri))
                }
            }
        }
    }

    /**
     * Toggles favorite status of the item.
     * Adds to favorites if not favorited, removes if already favorited.
     */
    fun onFavoriteClick(item: AppMediaItem) {
        viewModelScope.launch {
            if (item.favorite == true) {
                apiClient.sendRequest(
                    Request.Library.removeFavorite(item.itemId, item.mediaType)
                )
            } else {
                item.uri?.let {
                    apiClient.sendRequest(Request.Library.addFavorite(it))
                }
            }
        }
    }

    suspend fun getEditablePlaylists(): List<AppMediaItem.Playlist> {
        val result = apiClient.sendRequest(Request.Playlist.listLibrary())
        return result.resultAs<List<ServerMediaItem>>()
            ?.toAppMediaItemList()
            ?.filterIsInstance<AppMediaItem.Playlist>()
            ?.filter { it.isEditable == true }
            ?: emptyList()
    }

    fun addToPlaylist(
        mediaItem: AppMediaItem,
        playlist: AppMediaItem.Playlist
    ) {
        viewModelScope.launch {
            val itemUri = mediaItem.uri
                ?: run {
                    _toasts.emit("Media item has no URI")
                    return@launch
                }
            apiClient.sendRequest(
                Request.Playlist.addTracks(
                    playlistId = playlist.itemId,
                    trackUris = listOf(itemUri),
                )
            )
                .map { "Added to ${playlist.name}" }
                .onSuccess { message ->
                    _toasts.emit(message)
                }
                .onFailure {
                    _toasts.emit("Error adding to playlist")
                }
        }
    }

    fun removeFromPlaylist(
        playlistId: String,
        position: Int,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            apiClient.sendRequest(
                Request.Playlist.removeTracks(
                    playlistId = playlistId,
                    positions = listOf(position + 1) // +1 because server uses 1-based indexing
                )
            ).onSuccess {
                onSuccess()
            }
        }
    }

    data class PlaylistAddingActions(
        val onLoadPlaylists: suspend () -> List<AppMediaItem.Playlist>,
        val onAddToPlaylist: (AppMediaItem, AppMediaItem.Playlist) -> Unit
    )

    data class LibraryActions(
        val onLibraryClick: ((AppMediaItem) -> Unit),
        val onFavoriteClick: ((AppMediaItem) -> Unit),
    )

}