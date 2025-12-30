@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.painters.VinylRecordPainter
import io.music_assistant.client.ui.compose.common.painters.WaveformPainter
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import io.music_assistant.client.utils.SessionState

@Composable
fun LandingPage(
    modifier: Modifier = Modifier,
    connectionState: SessionState,
    dataState: DataState<List<AppMediaItem.RecommendationFolder>>,
    serverUrl: String?,
    onItemClick: (AppMediaItem) -> Unit,
    onLibraryItemClick: (MediaType?) -> Unit,
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
                    onAllClick = { row.rowItemType?.let { onLibraryItemClick(it) } },
                    mediaItems = row.items.orEmpty()
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
            LibraryItem("Search", Icons.Default.Search, null),
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
                            name = item.name,
                            icon = item.icon,
                            onClick = { onLibraryItemClick(item.type) }
                        )
                    }
                    // Fill remaining columns with spacers to keep grid alignment
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.width(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItemCard(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val width = 96.dp
    val height = 48.dp
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
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
            modifier = Modifier.width(width),
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
    onAllClick: () -> Unit,
    mediaItems: List<AppMediaItem>
) {
    val isHomogenous = remember(mediaItems) {
        mediaItems.all { it::class == mediaItems.firstOrNull()?.let { first -> first::class } }
    }
    val onLongItemClick: (AppMediaItem) -> Unit = {/*TODO*/ }
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
                    is AppMediaItem.Track -> TrackItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    is AppMediaItem.Artist -> ArtistItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl,
                        homogenous = isHomogenous,
                    )

                    is AppMediaItem.Album -> AlbumItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    is AppMediaItem.Playlist -> PlaylistItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl,
                        homogenous = isHomogenous,
                    )

                    else -> {}
                }
            }
        }
    }
}

// --- Specific Item Composables ---
@Composable
fun LibraryItem(
    item: AppMediaItem,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
fun TrackItem(
    item: AppMediaItem.Track,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp))
                .background(primaryContainer)
        ) {
            val placeholder = rememberPlaceholderPainter(
                backgroundColor = primaryContainer,
                iconColor = onPrimaryContainer,
                icon = Icons.Default.MusicNote
            )
            AsyncImage(
                placeholder = placeholder,
                fallback = placeholder,
                model = item.imageInfo?.url(serverUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Draw waveform overlay at the bottom
            val waveformPainter = remember(onPrimaryContainer) {
                WaveformPainter(
                    waveColor = primary,
                    thickness = 3f
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
            ) {
                with(waveformPainter) {
                    draw(size)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
        Text(
            text = item.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ArtistItem(
    item: AppMediaItem.Artist,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
    homogenous: Boolean,
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(CircleShape)
                .background(primaryContainer)
        ) {
            val placeholder = rememberPlaceholderPainter(
                backgroundColor = primaryContainer,
                iconColor = onPrimaryContainer,
                icon = Icons.Default.Mic
            )
            AsyncImage(
                placeholder = placeholder,
                fallback = placeholder,
                model = item.imageInfo?.url(serverUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!homogenous) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AlbumItem(
    item: AppMediaItem.Album,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    val primaryContainer = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp))
        ) {

            val vinylRecord = remember(primaryContainer, background) {
                VinylRecordPainter(
                    recordColor = Color.DarkGray,
                    labelColor = primaryContainer,
                    holeColor = background
                )
            }
            val stripWidth = 10.dp
            val holeRadius = 16.dp

            val cutStripShape = remember(stripWidth) { CutStripShape(stripWidth) }
            val holeShape = remember(holeRadius) { HoleShape(holeRadius) }

            Image(
                painter = vinylRecord,
                contentDescription = "Vinyl Record",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )

            AsyncImage(
                placeholder = vinylRecord,
                fallback = vinylRecord,
                model = item.imageInfo?.url(serverUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cutStripShape)
                    .clip(holeShape)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.width(itemSize),
            text = item.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PlaylistItem(
    item: AppMediaItem.Playlist,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
    homogenous: Boolean,
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val placeholder = rememberPlaceholderPainter(
                backgroundColor = primaryContainer,
                iconColor = onPrimaryContainer,
                icon = Icons.AutoMirrored.Filled.FeaturedPlayList
            )
            AsyncImage(
                placeholder = placeholder,
                fallback = placeholder,
                model = item.imageInfo?.url(serverUrl),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (!homogenous) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

class CutStripShape(private val stripWidth: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val stripPx = with(density) { stripWidth.toPx() }

            // Defines the album cover area, excluding the rightmost strip
            moveTo(0f, 0f)
            lineTo(size.width - stripPx, 0f)
            lineTo(size.width - stripPx, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

class HoleShape(private val holeRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { holeRadius.toPx() }
        val center = size.width / 2f

        // 1. Define the full cover path (the shape we want to keep)
        val coverPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
        }

        // 2. Define the hole path (the shape we want to cut out)
        val holePath = Path().apply {
            val rect = Rect(
                left = center - radiusPx,
                top = center - radiusPx,
                right = center + radiusPx,
                bottom = center + radiusPx
            )
            // Use addOval to define the circle
            addOval(oval = rect)
        }

        // 3. Perform the subtraction operation (Difference)
        val finalPath = Path.combine(
            operation = PathOperation.Difference, // <-- This explicitly subtracts the second path
            path1 = coverPath, // The album cover
            path2 = holePath // The hole
        )

        return Outline.Generic(finalPath)
    }
}

fun allItemsTitle(type: MediaType) = when (type) {
    MediaType.TRACK -> "All tracks"
    MediaType.ALBUM -> "All albums"
    MediaType.ARTIST -> "All artists"
    MediaType.PLAYLIST -> "All playlists"
    else -> null
}


