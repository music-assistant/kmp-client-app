@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.OverflowMenu
import io.music_assistant.client.ui.compose.common.OverflowMenuOption
import io.music_assistant.client.ui.compose.common.items.AlbumImage
import io.music_assistant.client.ui.compose.common.items.ArtistImage
import io.music_assistant.client.ui.compose.common.items.PlaylistImage
import io.music_assistant.client.ui.compose.library2.AdaptiveMediaGrid
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ItemDetailsScreen(
    itemId: String,
    mediaType: MediaType,
    onBack: () -> Unit,
) {
    val viewModel: ItemDetailsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)

    LaunchedEffect(itemId, mediaType) {
        viewModel.loadItem(itemId, mediaType)
    }

    ItemDetailsContent(
        state = state,
        serverUrl = serverUrl,
        onBack = onBack,
        onFavoriteClick = viewModel::onFavoriteClick,
        onPlayClick = viewModel::onPlayClick,
        onSubItemClick = { /* TODO: Navigate or play */ },
        onSubItemLongClick = { /* TODO: Show context menu */ }
    )
}

@Composable
private fun ItemDetailsContent(
    state: ItemDetailsViewModel.State,
    serverUrl: String?,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayClick: (QueueOption) -> Unit,
    onSubItemClick: (AppMediaItem) -> Unit,
    onSubItemLongClick: (AppMediaItem) -> Unit,
) {

    Column {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        when (val itemState = state.itemState) {
            is DataState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DataState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error loading item",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is DataState.Data -> {
                val item = itemState.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header section
                    item {
                        HeaderSection(
                            item = item,
                            serverUrl = serverUrl,
                            onFavoriteClick = onFavoriteClick,
                            onPlayClick = onPlayClick
                        )
                    }

                    // For Artist: Albums section
                    if (item is AppMediaItem.Artist) {
                        when (val albumsState = state.albumsState) {
                            is DataState.Data -> {
                                if (albumsState.data.isNotEmpty()) {
                                    item {
                                        SectionHeader("Albums")
                                    }
                                    item {
                                        AdaptiveMediaGrid(
                                            items = albumsState.data,
                                            serverUrl = serverUrl,
                                            onItemClick = onSubItemClick,
                                            onItemLongClick = onSubItemLongClick
                                        )
                                    }
                                }
                            }

                            is DataState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }

                    // Tracks section (all types)
                    when (val tracksState = state.tracksState) {
                        is DataState.Data -> {
                            if (tracksState.data.isNotEmpty()) {
                                item {
                                    SectionHeader("Tracks")
                                }
                                item {
                                    AdaptiveMediaGrid(
                                        items = tracksState.data,
                                        serverUrl = serverUrl,
                                        onItemClick = onSubItemClick,
                                        onItemLongClick = onSubItemLongClick
                                    )
                                }
                            }
                        }

                        is DataState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        else -> Unit
                    }
                }
            }

            is DataState.NoData -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available")
                }
            }
        }
    }

}

@Composable
private fun HeaderSection(
    item: AppMediaItem,
    serverUrl: String?,
    onFavoriteClick: () -> Unit,
    onPlayClick: (QueueOption) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (item) {
            is AppMediaItem.Artist -> ArtistImage(128.dp, item, serverUrl)
            is AppMediaItem.Album -> AlbumImage(128.dp, item, serverUrl)
            is AppMediaItem.Playlist -> PlaylistImage(128.dp, item, serverUrl)
            else -> Unit
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Button(
                    onClick = { onPlayClick(QueueOption.PLAY) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play now")
                }

                OverflowMenu(
                    modifier = Modifier,
                    buttonContent = { onClick ->
                        Icon(
                            modifier = Modifier.clickable { onClick() },
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    },
                    options = listOf(
                        OverflowMenuOption("Play Next") { onPlayClick(QueueOption.NEXT) },
                        OverflowMenuOption("Add to Queue") { onPlayClick(QueueOption.ADD) },
                        OverflowMenuOption("Replace Queue") { onPlayClick(QueueOption.REPLACE) }
                    )
                )

                val isFavorite = item.favorite == true
                Icon(
                    modifier = Modifier.clickable{ onFavoriteClick() },
                    imageVector = if (isFavorite) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFEF7BC4)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp, 8.dp)
    )
}
