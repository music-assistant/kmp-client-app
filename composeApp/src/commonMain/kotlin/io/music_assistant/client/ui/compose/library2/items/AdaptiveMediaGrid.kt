package io.music_assistant.client.ui.compose.library2.items

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.AppMediaItem

@Composable
fun AdaptiveMediaGrid(
    modifier: Modifier = Modifier,
    items: List<AppMediaItem>,
    serverUrl: String?,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onItemClick: (AppMediaItem) -> Unit,
    onItemLongClick: (AppMediaItem) -> Unit,
    onLoadMore: () -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(),
) {

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
        modifier = modifier,
        state = gridState,
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.itemId }) { item ->
            when (item) {
                is AppMediaItem.Track -> GridTrackItem(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
                is AppMediaItem.Artist -> GridArtistItem(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
                is AppMediaItem.Album -> GridAlbumItem(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
                is AppMediaItem.Playlist -> GridPlaylistItem(
                    item = item,
                    serverUrl = serverUrl,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
