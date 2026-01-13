package io.music_assistant.client.ui.compose.library

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.items.MediaItemAlbum
import io.music_assistant.client.ui.compose.common.items.MediaItemArtist
import io.music_assistant.client.ui.compose.common.items.MediaItemPlaylist
import io.music_assistant.client.ui.compose.common.items.TrackItemWithMenu
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import kotlinx.coroutines.launch

@Composable
fun AdaptiveMediaGrid(
    modifier: Modifier = Modifier,
    items: List<AppMediaItem>,
    serverUrl: String?,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackClick: ((AppMediaItem.Track, QueueOption) -> Unit),
    onLoadMore: () -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(),
    playlistActions: ActionsViewModel.PlaylistActions,
    libraryActions: ActionsViewModel.LibraryActions,
) {
    val coroutineScope = rememberCoroutineScope()

    // Detect when we're near the end and trigger load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // Load more when we're within 10 items of the end
            hasMore && !isLoadingMore && totalItems > 0 && lastVisibleItem >= totalItems - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        modifier = modifier
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        gridState.scrollBy(-delta)
                    }
                },
            ),
        state = gridState,
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.itemId }) { item ->
            when (item) {
                is AppMediaItem.Track -> {
                    TrackItemWithMenu(
                        item = item,
                        serverUrl = serverUrl,
                        onTrackPlayOption = onTrackClick,
                        onItemClick = { onItemClick(it) },
                        playlistActions = playlistActions,
                        libraryActions = libraryActions,
                        providerIconFetcher = null
                    )
                }

                is AppMediaItem.Artist -> MediaItemArtist(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(it) },
                    providerIconFetcher = null
                )

                is AppMediaItem.Album -> MediaItemAlbum(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(it) },
                    providerIconFetcher = null
                )

                is AppMediaItem.Playlist -> MediaItemPlaylist(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(it) },
                    providerIconFetcher = null
                )

                else -> {
                    // Unsupported item type - skip
                }
            }
        }

        // Loading indicator at the bottom
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
