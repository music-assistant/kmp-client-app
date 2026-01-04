package io.music_assistant.client.ui.compose.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import kotlinx.coroutines.launch

/**
 * VM that provides library and favorites actions for media items.
 * Can be used by any view that needs to add/remove items from library or favorites.
 */
class LibraryActionsViewModel(
    private val apiClient: ServiceClient
): ViewModel() {
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
}