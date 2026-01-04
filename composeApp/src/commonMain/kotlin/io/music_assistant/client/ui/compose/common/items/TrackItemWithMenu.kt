package io.music_assistant.client.ui.compose.common.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.QueueOption
import kotlinx.coroutines.launch

/**
 * A reusable composable that displays a track item with a dropdown menu for queue actions.
 * When onTrackClick is provided, clicking the item opens a menu with play options.
 * Otherwise, it behaves as a simple clickable track item.
 */
@Composable
fun TrackItemWithMenu(
    modifier: Modifier = Modifier,
    item: AppMediaItem.Track,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((AppMediaItem.Track, QueueOption) -> Unit),
    onItemClick: ((AppMediaItem.Track) -> Unit)? = null,
    playlistAddingParameters: PlaylistAddingParameters? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    showProvider: Boolean = false,
    serverUrl: String?
) {
    var expandedTrackId by remember { mutableStateOf<String?>(null) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<AppMediaItem.Playlist>>(emptyList()) }
    var isLoadingPlaylists by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        MediaItemTrack(
            modifier = Modifier.align(Alignment.Center),
            item = item,
            serverUrl = serverUrl,
            onClick = { expandedTrackId = item.itemId },
            itemSize = itemSize,
            showProvider = showProvider,
        )
        DropdownMenu(
            expanded = expandedTrackId == item.itemId,
            onDismissRequest = { expandedTrackId = null }
        ) {
            DropdownMenuItem(
                text = { Text("Play Now") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.PLAY)
                    expandedTrackId = null
                }
            )
            DropdownMenuItem(
                text = { Text("Play Next") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.NEXT)
                    expandedTrackId = null
                }
            )
            DropdownMenuItem(
                text = { Text("Add to Queue") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.ADD)
                    expandedTrackId = null
                }
            )
            DropdownMenuItem(
                text = { Text("Replace Queue") },
                onClick = {
                    onTrackPlayOption(item, QueueOption.REPLACE)
                    expandedTrackId = null
                }
            )
            if (playlistAddingParameters != null) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        showPlaylistDialog = true
                        expandedTrackId = null
                        // Load playlists when dialog opens
                        coroutineScope.launch {
                            isLoadingPlaylists = true
                            playlists = playlistAddingParameters.onLoadPlaylists()
                            isLoadingPlaylists = false
                        }
                    }
                )
            }
            if (onRemoveFromPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("Remove from Playlist") },
                    onClick = {
                        onRemoveFromPlaylist()
                        expandedTrackId = null
                    }
                )
            }
        }

        // Add to Playlist Dialog
        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    playlists = emptyList()
                    isLoadingPlaylists = false
                },
                title = { Text("Add to Playlist") },
                text = {
                    if (isLoadingPlaylists) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (playlists.isEmpty()) {
                        Text("No editable playlists available")
                    } else {
                        Column {
                            playlists.forEach { playlist ->
                                TextButton(
                                    onClick = {
                                        playlistAddingParameters?.onAddToPlaylist
                                            ?.invoke(item, playlist)
                                        showPlaylistDialog = false
                                        playlists = emptyList()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        showPlaylistDialog = false
                        playlists = emptyList()
                        isLoadingPlaylists = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

data class PlaylistAddingParameters(
    val onLoadPlaylists: suspend () -> List<AppMediaItem.Playlist>,
    val onAddToPlaylist: (AppMediaItem, AppMediaItem.Playlist) -> Unit
)
