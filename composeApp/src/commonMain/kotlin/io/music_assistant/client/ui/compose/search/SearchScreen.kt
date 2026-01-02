@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.items.MediaItemAlbum
import io.music_assistant.client.ui.compose.common.items.MediaItemArtist
import io.music_assistant.client.ui.compose.common.items.MediaItemPlaylist
import io.music_assistant.client.ui.compose.common.items.TrackItemWithMenu
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToItem: (String, MediaType, String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)

    SearchContent(
        state = state,
        serverUrl = serverUrl,
        onBack = onBack,
        onQueryChanged = viewModel::onQueryChanged,
        onMediaTypeToggled = viewModel::onMediaTypeToggled,
        onLibraryOnlyToggled = viewModel::onLibraryOnlyToggled,
        onItemClick = { item ->
            when (item) {
                is AppMediaItem.Artist,
                is AppMediaItem.Album,
                is AppMediaItem.Playlist -> {
                    onNavigateToItem(item.itemId, item.mediaType, item.provider)
                }

                else -> Unit
            }
        },
        onTrackClick = viewModel::onTrackClick
    )
}

@Composable
private fun SearchContent(
    state: SearchViewModel.State,
    serverUrl: String?,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onMediaTypeToggled: (MediaType, Boolean) -> Unit,
    onLibraryOnlyToggled: (Boolean) -> Unit,
    onItemClick: (AppMediaItem) -> Unit,
    onTrackClick: (AppMediaItem.Track, io.music_assistant.client.data.model.server.QueueOption) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            // Search input
            OutlinedTextField(
                modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                value = state.searchState.query,
                onValueChange = onQueryChanged,
                label = {
                    Text(
                        text = if (state.searchState.query.trim().length < 3)
                            "Type at least 3 characters to search"
                        else
                            "Search query"
                    )
                },
            )
        }

        // Search filters (always visible)
        SearchFilters(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            searchState = state.searchState,
            onMediaTypeToggled = onMediaTypeToggled,
            onLibraryOnlyToggled = onLibraryOnlyToggled
        )

        // Results
        when (val resultsState = state.resultsState) {
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
                        text = "Error loading search results",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is DataState.Data -> {
                val results = resultsState.data
                val hasResults = results.artists.isNotEmpty() ||
                        results.albums.isNotEmpty() ||
                        results.tracks.isNotEmpty() ||
                        results.playlists.isNotEmpty()

                if (!hasResults) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results found")
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Artists section
                        if (results.artists.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader("Artists")
                            }
                            items(results.artists) { artist ->
                                MediaItemArtist(
                                    item = artist,
                                    serverUrl = serverUrl,
                                    onClick = { onItemClick(it) }
                                )
                            }
                        }

                        // Albums section
                        if (results.albums.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader("Albums")
                            }
                            items(results.albums) { album ->
                                MediaItemAlbum(
                                    item = album,
                                    serverUrl = serverUrl,
                                    onClick = { onItemClick(it) }
                                )
                            }
                        }

                        // Tracks section
                        if (results.tracks.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader("Tracks")
                            }
                            items(results.tracks) { track ->
                                TrackItemWithMenu(
                                    item = track,
                                    serverUrl = serverUrl,
                                    onTrackPlayOption = onTrackClick
                                )
                            }
                        }

                        // Playlists section
                        if (results.playlists.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader("Playlists")
                            }
                            items(results.playlists) { playlist ->
                                MediaItemPlaylist(
                                    item = playlist,
                                    serverUrl = serverUrl,
                                    onClick = { onItemClick(it) }
                                )
                            }
                        }
                    }
                }
            }

            is DataState.NoData -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Type at least 3 characters to search")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilters(
    modifier: Modifier = Modifier,
    searchState: SearchViewModel.SearchState,
    onMediaTypeToggled: (MediaType, Boolean) -> Unit,
    onLibraryOnlyToggled: (Boolean) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Media type filter chips
        searchState.mediaTypes.forEach { mediaTypeSelect ->
            FilterChip(
                selected = mediaTypeSelect.isSelected,
                onClick = {
                    onMediaTypeToggled(
                        mediaTypeSelect.type,
                        !mediaTypeSelect.isSelected
                    )
                },
                label = {
                    Text(mediaTypeSelect.type.name.lowercase().capitalize(Locale.current))
                }
            )
        }

        // In library only filter chip
        FilterChip(
            selected = searchState.libraryOnly,
            onClick = { onLibraryOnlyToggled(!searchState.libraryOnly) },
            label = {
                Text("In library only")
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
