@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.items.MediaItemAlbum
import io.music_assistant.client.ui.compose.common.items.MediaItemArtist
import io.music_assistant.client.ui.compose.common.items.MediaItemPlaylist
import io.music_assistant.client.ui.compose.common.items.PlaylistAddingParameters
import io.music_assistant.client.ui.compose.common.items.TrackItemWithMenu
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import io.music_assistant.client.utils.SessionState

@Composable
fun LandingPage(
    modifier: Modifier = Modifier,
    connectionState: SessionState,
    dataState: DataState<List<AppMediaItem.RecommendationFolder>>,
    serverUrl: String?,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackPlayOption: ((AppMediaItem.Track, QueueOption) -> Unit)? = null,
    onLibraryItemClick: (MediaType?) -> Unit,
    playlistAddingParameters: PlaylistAddingParameters,
) {
    val filteredData = remember(dataState) {
        if (dataState is DataState.Data) {
            dataState.data.filter {
                it.items?.any { item ->
                    item is AppMediaItem.Track
                            || item is AppMediaItem.Artist
                            || item is AppMediaItem.Album
                            || item is AppMediaItem.Playlist
                } == true
            }
        } else {
            emptyList()
        }
    }

    if (connectionState !is SessionState.Connected || dataState !is DataState.Data) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Your library row
            item {
                LibraryRow(onLibraryItemClick = onLibraryItemClick)
            }

            items(
                items = filteredData,
                key = { it.itemId }
            ) { row ->
                Logger.e("Row ID : ${row.itemId}, Name: ${row.name}, Items count: ${row.items?.size}")
                CategoryRow(
                    serverUrl = serverUrl,
                    row = row,
                    onItemClick = onItemClick,
                    onTrackPlayOption = onTrackPlayOption,
                    onAllClick = { row.rowItemType?.let { onLibraryItemClick(it) } },
                    mediaItems = row.items.orEmpty(),
                    playlistAddingParameters = playlistAddingParameters,
                )
            }
        }
    }
}

// --- Common UI Components ---

@Composable
fun LibraryRow(
    onLibraryItemClick: (MediaType?) -> Unit
) {
    val libraryItems = remember {
        listOf(
            LibraryItem("Artists", Icons.Default.Mic, MediaType.ARTIST),
            LibraryItem("Albums", Icons.Default.Album, MediaType.ALBUM),
            LibraryItem("Tracks", Icons.Default.MusicNote, MediaType.TRACK),
            LibraryItem(
                "Playlists",
                Icons.AutoMirrored.Filled.FeaturedPlayList,
                MediaType.PLAYLIST
            ),
            LibraryItem("Global search", Icons.Default.Search, null),
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your library",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            libraryItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { item ->
                        LibraryItemCard(
                            modifier = Modifier.weight(1f),
                            name = item.name,
                            icon = item.icon,
                            onClick = { onLibraryItemClick(item.type) }
                        )
                    }
                    // Fill remaining columns with spacers to keep grid alignment
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f).padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItemCard(
    modifier: Modifier,
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val placeholder = rememberPlaceholderPainter(
                backgroundColor = primaryContainer,
                iconColor = onPrimaryContainer,
                icon = icon
            )
            Image(
                painter = placeholder,
                contentDescription = name,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private data class LibraryItem(
    val name: String,
    val icon: ImageVector,
    val type: MediaType?,
)

@Composable
fun CategoryRow(
    serverUrl: String?,
    row: AppMediaItem.RecommendationFolder,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackPlayOption: ((AppMediaItem.Track, QueueOption) -> Unit)?,
    onAllClick: () -> Unit,
    mediaItems: List<AppMediaItem>,
    playlistAddingParameters: PlaylistAddingParameters,
) {
    val isHomogenous = remember(mediaItems) {
        mediaItems.all { it::class == mediaItems.firstOrNull()?.let { first -> first::class } }
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleLarge
            )
            row.rowItemType?.let { type ->
                val title = allItemsTitle(type)
                title?.let {
                    TextButton(
                        onClick = onAllClick,
                        contentPadding = PaddingValues(start = 4.dp, end = 4.dp)
                    ) {
                        Text(title, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = mediaItems,
                key = { item ->
                    when (item) {
                        is AppMediaItem.Track -> "track_${item.itemId}"
                        is AppMediaItem.Artist -> "artist_${item.itemId}"
                        is AppMediaItem.Album -> "album_${item.itemId}"
                        is AppMediaItem.Playlist -> "playlist_${item.itemId}"
                        else -> item.hashCode()
                    }
                },
                contentType = { item ->
                    when (item) {
                        is AppMediaItem.Track -> "Track"
                        is AppMediaItem.Artist -> "Artist"
                        is AppMediaItem.Album -> "Album"
                        is AppMediaItem.Playlist -> "Playlist"
                        else -> "Unknown"
                    }
                }
            ) { item ->
                when (item) {
                    is AppMediaItem.Track -> {
                        TrackItemWithMenu(
                            item = item,
                            serverUrl = serverUrl,
                            itemSize = 96.dp,
                            onTrackPlayOption = onTrackPlayOption,
                            onItemClick = { onItemClick(it) },
                            playlistAddingParameters = playlistAddingParameters,
                        )
                    }

                    is AppMediaItem.Artist -> MediaItemArtist(
                        item = item,
                        serverUrl = serverUrl,
                        onClick = { onItemClick(it) },
                        itemSize = 96.dp,
                        showSubtitle = !isHomogenous
                    )

                    is AppMediaItem.Album -> MediaItemAlbum(
                        item = item,
                        serverUrl = serverUrl,
                        onClick = { onItemClick(it) },
                        itemSize = 96.dp
                    )

                    is AppMediaItem.Playlist -> MediaItemPlaylist(
                        item = item,
                        serverUrl = serverUrl,
                        onClick = { onItemClick(it) },
                        itemSize = 96.dp,
                        showSubtitle = !isHomogenous
                    )

                    else -> {}
                }
            }
        }
    }
}

fun allItemsTitle(type: MediaType) = when (type) {
    MediaType.TRACK -> "All tracks"
    MediaType.ALBUM -> "All albums"
    MediaType.ARTIST -> "All artists"
    MediaType.PLAYLIST -> "All playlists"
    else -> null
}


