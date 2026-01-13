@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.OverflowMenu
import io.music_assistant.client.ui.compose.common.OverflowMenuOption
import io.music_assistant.client.ui.compose.common.ToastHost
import io.music_assistant.client.ui.compose.common.ToastState
import io.music_assistant.client.ui.compose.common.items.AlbumImage
import io.music_assistant.client.ui.compose.common.items.ArtistImage
import io.music_assistant.client.ui.compose.common.items.Badges
import io.music_assistant.client.ui.compose.common.items.MediaItemAlbum
import io.music_assistant.client.ui.compose.common.items.PlaylistImage
import io.music_assistant.client.ui.compose.common.items.TrackItemWithMenu
import io.music_assistant.client.ui.compose.common.providers.ProviderIcon
import io.music_assistant.client.ui.compose.common.rememberToastState
import io.music_assistant.client.ui.compose.common.viewmodel.ActionsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ItemDetailsScreen(
    itemId: String,
    mediaType: MediaType,
    providerId: String,
    onBack: () -> Unit,
    onNavigateToItem: (String, MediaType, String) -> Unit,
) {
    val viewModel: ItemDetailsViewModel = koinViewModel()
    val actionsViewModel: ActionsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)
    val toastState = rememberToastState()

    LaunchedEffect(itemId, mediaType) {
        viewModel.loadItem(itemId, mediaType, providerId)
    }

    // Collect toasts
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { toast ->
            toastState.showToast(toast)
        }
    }

    ItemDetailsContent(
        state = state,
        serverUrl = serverUrl,
        toastState = toastState,
        onBack = onBack,
        onPlayClick = viewModel::onPlayClick,
        onSubItemClick = { item ->
            when (item) {
                is AppMediaItem.Artist,
                is AppMediaItem.Album,
                is AppMediaItem.Playlist -> {
                    onNavigateToItem(item.itemId, item.mediaType, item.provider)
                }

                else -> Unit
            }
        },
        onTrackClick = viewModel::onTrackClick,
        playlistActions = ActionsViewModel.PlaylistActions(
            onLoadPlaylists = actionsViewModel::getEditablePlaylists,
            onAddToPlaylist = actionsViewModel::addToPlaylist
        ),
        onRemoveFromPlaylist = { id, pos ->
            actionsViewModel.removeFromPlaylist(
                id,
                pos,
                viewModel::reload
            )
        },
        libraryActions = ActionsViewModel.LibraryActions(
            onLibraryClick = actionsViewModel::onLibraryClick,
            onFavoriteClick = actionsViewModel::onFavoriteClick
        ),
        providerIconFetcher = { modifier, provider ->
            actionsViewModel.getProviderIcon(provider)
                ?.let { ProviderIcon(modifier, it) }
        }
    )
}

@Composable
private fun ItemDetailsContent(
    state: ItemDetailsViewModel.State,
    serverUrl: String?,
    toastState: ToastState,
    onBack: () -> Unit,
    onPlayClick: (QueueOption) -> Unit,
    onSubItemClick: (AppMediaItem) -> Unit,
    onTrackClick: (AppMediaItem.Track, QueueOption) -> Unit,
    playlistActions: ActionsViewModel.PlaylistActions,
    onRemoveFromPlaylist: (String, Int) -> Unit,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: (@Composable (Modifier, String) -> Unit),
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                val coroutineScope = rememberCoroutineScope()
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize()
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
                    // Header section - spans full width
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HeaderSection(
                            item = item,
                            serverUrl = serverUrl,
                            onPlayClick = onPlayClick,
                            playlistActions = playlistActions.takeIf { item is AppMediaItem.Track || item is AppMediaItem.Album },
                            libraryActions = libraryActions,
                            providerIconFetcher = providerIconFetcher,
                        )
                    }

                    // For Artist: Albums section
                    if (item is AppMediaItem.Artist) {
                        when (val albumsState = state.albumsState) {
                            is DataState.Data -> {
                                if (albumsState.data.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader("Albums")
                                    }
                                    items(albumsState.data) { album ->
                                        MediaItemAlbum(
                                            item = album,
                                            serverUrl = serverUrl,
                                            onClick = { onSubItemClick(album) },
                                            providerIconFetcher = providerIconFetcher,
                                        )
                                    }
                                }
                            }

                            is DataState.Loading -> {
                                item(span = { GridItemSpan(maxLineSpan) }) {
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
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader("Tracks")
                                }
                                tracksState.data.forEachIndexed { index, track ->
                                    item {
                                        TrackItemWithMenu(
                                            item = track,
                                            serverUrl = serverUrl,
                                            onTrackPlayOption = onTrackClick,
                                            // Don't show "add to playlist" for playlist items
                                            playlistActions = playlistActions
                                                .takeIf { item !is AppMediaItem.Playlist },
                                            // Show "remove from playlist" only for playlist items
                                            onRemoveFromPlaylist = if (item is AppMediaItem.Playlist && item.isEditable == true) {
                                                { onRemoveFromPlaylist(item.itemId, index) }
                                            } else null,
                                            libraryActions = libraryActions,
                                            providerIconFetcher = providerIconFetcher,
                                        )
                                    }
                                }
                            }
                        }

                        is DataState.Loading -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
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

        // Toast host
        ToastHost(
            toastState = toastState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
        )
        // Place it here so it'd be clickable
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
    }
}

@Composable
private fun HeaderSection(
    item: AppMediaItem,
    serverUrl: String?,
    onPlayClick: (QueueOption) -> Unit,
    playlistActions: ActionsViewModel.PlaylistActions?,
    libraryActions: ActionsViewModel.LibraryActions,
    providerIconFetcher: @Composable ((Modifier, String) -> Unit)?,

    ) {
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<AppMediaItem.Playlist>>(emptyList()) }
    var isLoadingPlaylists by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .padding(start = 40.dp, end = 16.dp)
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box {
            when (item) {
                is AppMediaItem.Artist -> ArtistImage(128.dp, item, serverUrl)
                is AppMediaItem.Album -> AlbumImage(128.dp, item, serverUrl)
                is AppMediaItem.Playlist -> PlaylistImage(128.dp, item, serverUrl)
                else -> Unit
            }
            Badges(
                item = item,
                providerIconFetcher = providerIconFetcher
            )
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    options = buildList {
                        add(OverflowMenuOption("Play Next") { onPlayClick(QueueOption.NEXT) })
                        add(OverflowMenuOption("Add to Queue") { onPlayClick(QueueOption.ADD) })
                        add(OverflowMenuOption("Replace Queue") { onPlayClick(QueueOption.REPLACE) })
                        add(
                            OverflowMenuOption(
                                if (item.isInLibrary) "Remove from library"
                                else "Add to library"
                            ) { libraryActions.onLibraryClick(item) })
                        if (item.isInLibrary) {
                            add(
                                OverflowMenuOption(
                                    if (item.favorite == true) "Unfavorite"
                                    else "Favorite"
                                ) { libraryActions.onFavoriteClick(item) })
                        }
                        playlistActions?.let {
                            add(OverflowMenuOption("Add to Playlist") {
                                showPlaylistDialog = true
                                // Load playlists when dialog opens
                                coroutineScope.launch {
                                    isLoadingPlaylists = true
                                    playlists = it.onLoadPlaylists()
                                    isLoadingPlaylists = false
                                }
                            })
                        }
                    }
                )
            }
        }
    }

    // Add to Playlist dialog
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
                                    playlistActions?.onAddToPlaylist(item, playlist)
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(16.dp, 8.dp)
    )
}
