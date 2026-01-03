@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.library2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.ToastHost
import io.music_assistant.client.ui.compose.common.items.PlaylistAddingParameters
import io.music_assistant.client.ui.compose.common.rememberToastState
import org.koin.compose.koinInject

@Composable
fun Library2Screen(
    initialTabType: MediaType?,
    onBack: () -> Unit,
    onItemClick: (AppMediaItem) -> Unit,
) {
    val viewModel: Library2ViewModel = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)
    val toastState = rememberToastState()

    // Map MediaType to Tab
    val initialTab = when (initialTabType) {
        MediaType.ARTIST -> Library2ViewModel.Tab.ARTISTS
        MediaType.ALBUM -> Library2ViewModel.Tab.ALBUMS
        MediaType.TRACK -> Library2ViewModel.Tab.TRACKS
        MediaType.PLAYLIST -> Library2ViewModel.Tab.PLAYLISTS
        null -> Library2ViewModel.Tab.ARTISTS
        else -> Library2ViewModel.Tab.ARTISTS
    }

    // Set initial tab
    LaunchedEffect(initialTab) {
        viewModel.onTabSelected(initialTab)
    }

    // Collect toasts
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { toast ->
            toastState.showToast(toast)
        }
    }

    Library2(
        state = state,
        serverUrl = serverUrl,
        toastState = toastState,
        onBack = onBack,
        onTabSelected = viewModel::onTabSelected,
        onItemClick = onItemClick,
        onTrackClick = viewModel::onTrackClick,
        onCreatePlaylistClick = viewModel::onCreatePlaylistClick,
        onLoadMore = viewModel::loadMore,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onOnlyFavoritesClicked = viewModel::onOnlyFavoritesClicked,
        onDismissCreatePlaylistDialog = viewModel::onDismissCreatePlaylistDialog,
        onCreatePlaylist = viewModel::createPlaylist,
        playlistAddingParameters = PlaylistAddingParameters(
            onLoadPlaylists = viewModel::getEditablePlaylists,
            onAddToPlaylist = viewModel::addToPlaylist
        )
    )
}

@Composable
private fun Library2(
    state: Library2ViewModel.State,
    serverUrl: String?,
    toastState: io.music_assistant.client.ui.compose.common.ToastState,
    onBack: () -> Unit,
    onTabSelected: (Library2ViewModel.Tab) -> Unit,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackClick: (AppMediaItem.Track, QueueOption) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onLoadMore: (Library2ViewModel.Tab) -> Unit,
    onSearchQueryChanged: (Library2ViewModel.Tab, String) -> Unit,
    onOnlyFavoritesClicked: (Library2ViewModel.Tab) -> Unit,
    onDismissCreatePlaylistDialog: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    playlistAddingParameters: PlaylistAddingParameters,
) {
    val selectedTab = state.tabs.find { it.isSelected } ?: state.tabs.first()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                // Tab row
                PrimaryTabRow(
                    selectedTabIndex = state.tabs.indexOfFirst { it.isSelected }
                ) {
                    state.tabs.forEach { tabState ->
                        Tab(
                            selected = tabState.isSelected,
                            onClick = { onTabSelected(tabState.tab) },
                            text = {
                                Text(
                                    when (tabState.tab) {
                                        Library2ViewModel.Tab.ARTISTS -> "Artists"
                                        Library2ViewModel.Tab.ALBUMS -> "Albums"
                                        Library2ViewModel.Tab.TRACKS -> "Tracks"
                                        Library2ViewModel.Tab.PLAYLISTS -> "Playlists"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Quick search input
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = selectedTab.searchQuery,
                onValueChange = { onSearchQueryChanged(selectedTab.tab, it) },
                label = {
                    Text(text = "Quick search")
                },
                singleLine = true
            )
            FilterChip(
                modifier = Modifier.padding(horizontal = 16.dp),
                selected = selectedTab.onlyFavorites,
                onClick = { onOnlyFavoritesClicked(selectedTab.tab) },
                label = {
                    Text("Favorites")
                }
            )

            // Content area
            Box(modifier = Modifier.fillMaxSize()) {
                TabContent(
                    tabState = selectedTab,
                    serverUrl = serverUrl,
                    onItemClick = onItemClick,
                    onTrackClick = onTrackClick,
                    onCreatePlaylistClick = onCreatePlaylistClick,
                    onLoadMore = { onLoadMore(selectedTab.tab) },
                    playlistAddingParameters = playlistAddingParameters
                )
            }
        }

        // Toast host
        ToastHost(
            toastState = toastState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
        )

        // Create Playlist Dialog
        if (state.showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = onDismissCreatePlaylistDialog,
                onCreate = onCreatePlaylist
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Playlist") },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.trim().isNotEmpty()) {
                        onCreate(playlistName.trim())
                    }
                },
                enabled = playlistName.trim().isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TabContent(
    tabState: Library2ViewModel.TabState,
    serverUrl: String?,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackClick: (AppMediaItem.Track, QueueOption) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onLoadMore: () -> Unit,
    playlistAddingParameters: PlaylistAddingParameters
) {
    // Create separate grid states for each tab to preserve scroll position
    val artistsGridState = rememberLazyGridState()
    val albumsGridState = rememberLazyGridState()
    val tracksGridState = rememberLazyGridState()
    val playlistsGridState = rememberLazyGridState()

    val gridStates =
        remember(artistsGridState, albumsGridState, tracksGridState, playlistsGridState) {
            mapOf(
                Library2ViewModel.Tab.ARTISTS to artistsGridState,
                Library2ViewModel.Tab.ALBUMS to albumsGridState,
                Library2ViewModel.Tab.TRACKS to tracksGridState,
                Library2ViewModel.Tab.PLAYLISTS to playlistsGridState
            )
        }

    when (tabState.dataState) {
        is DataState.Loading -> LoadingState()
        is DataState.Error -> ErrorState()
        is DataState.NoData -> EmptyState()
        is DataState.Data -> {
            val items = tabState.dataState.data
            if (items.isEmpty()) {
                EmptyState()
            } else {
                key(tabState.tab) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (tabState.tab == Library2ViewModel.Tab.PLAYLISTS) {
                            OutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                onClick = onCreatePlaylistClick
                            ) {
                                Icon(TablerIcons.Plus, contentDescription = "Add playlist")
                                Spacer(Modifier.width(4.dp))
                                Text("Add new")
                            }
                        }
                        gridStates[tabState.tab]?.let {
                            AdaptiveMediaGrid(
                                modifier = Modifier.fillMaxSize(),
                                items = items,
                                serverUrl = serverUrl,
                                isLoadingMore = tabState.isLoadingMore,
                                hasMore = tabState.hasMore,
                                onItemClick = onItemClick,
                                onTrackClick = onTrackClick,
                                onLoadMore = onLoadMore,
                                gridState = it,
                                playlistAddingParameters = playlistAddingParameters
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error loading data",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No items found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
