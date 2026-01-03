package io.music_assistant.client.ui.compose.library

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.regular.Heart
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.Heart
import compose.icons.tablericons.CircleDashed
import compose.icons.tablericons.FileMusic
import compose.icons.tablericons.Folder
import compose.icons.tablericons.List
import compose.icons.tablericons.Man
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.PlayerTrackNext
import compose.icons.tablericons.Playlist
import compose.icons.tablericons.Plus
import compose.icons.tablericons.QuestionMark
import compose.icons.tablericons.Replace
import compose.icons.tablericons.Square
import compose.icons.tablericons.SquareCheck
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.ActionButton
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.OverflowMenu
import io.music_assistant.client.ui.compose.common.OverflowMenuOption
import io.music_assistant.client.ui.compose.common.ToastHost
import io.music_assistant.client.ui.compose.common.ToastState
import io.music_assistant.client.ui.compose.common.VerticalHidingContainer
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import io.music_assistant.client.ui.compose.common.rememberToastState
import io.music_assistant.client.ui.compose.nav.BackHandler
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(type: MediaType?, onBack: () -> Unit) {
    val toastState = rememberToastState()
    val viewModel = koinViewModel<LibraryViewModel>()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedList = state.libraryLists.firstOrNull { it.isSelected }
    BackHandler(enabled = true) {
        selectedList?.let {
            if (it.parentItems.isNotEmpty()) {
                viewModel.onUpClick(it.tab)
                return@BackHandler
            } else if (state.checkedItems.isNotEmpty()) {
                viewModel.clearCheckedItems()
                return@BackHandler
            }
        }
        onBack()
    }
    LaunchedEffect(Unit) { viewModel.toasts.collect { t -> toastState.showToast(t) } }
    Library(
        toastState = toastState,
        serverUrl = serverUrl,
        state = state,
        selectedList = selectedList,
        onBack = onBack,
        onListSelected = viewModel::onTabSelected,
        onItemClicked = viewModel::onItemClicked,
        onCheckChanged = viewModel::onItemCheckChanged,
        onFavoriteChanged = viewModel::onItemFavoriteChanged,
        onAddToLibrary = viewModel::onAddToLibrary,
        onCheckedItemsClear = viewModel::clearCheckedItems,
        onSearchQueryChanged = viewModel::searchQueryChanged,
        onSearchTypeChanged = viewModel::searchTypeChanged,
        onSearchLibraryOnlyChanged = viewModel::searchLibraryOnlyChanged,
        onUpClick = viewModel::onUpClick,
        onShowAlbumsChange = viewModel::onShowAlbumsChange,
        onPlaySelectedItems = { option ->
            viewModel.playSelectedItems(option)
            onBack()
        },
        onCreatePlaylist = viewModel::createPlaylist,
        onAddToPlaylist = viewModel::addToPlaylist
    )
}

@Composable
private fun Library(
    modifier: Modifier = Modifier,
    toastState: ToastState,
    serverUrl: String?,
    state: LibraryViewModel.State,
    selectedList: LibraryViewModel.LibraryList?,
    onBack: () -> Unit,
    onListSelected: (LibraryViewModel.LibraryTab) -> Unit,
    onItemClicked: (LibraryViewModel.LibraryTab, AppMediaItem) -> Unit,
    onCheckChanged: (AppMediaItem) -> Unit,
    onFavoriteChanged: (AppMediaItem) -> Unit,
    onAddToLibrary: (AppMediaItem) -> Unit,
    onCheckedItemsClear: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeChanged: (MediaType, Boolean) -> Unit,
    onSearchLibraryOnlyChanged: (Boolean) -> Unit,
    onUpClick: (LibraryViewModel.LibraryTab) -> Unit,
    onShowAlbumsChange: (Boolean) -> Unit,
    onPlaySelectedItems: (QueueOption) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (AppMediaItem.Track, AppMediaItem.Playlist) -> Unit,
) {
    val isFabVisible = rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember(selectedList?.parentItems) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < -1 && selectedList?.parentItems?.lastOrNull()?.mediaType == MediaType.ARTIST) {
                    isFabVisible.value = false
                } else if (available.y > 1) {
                    isFabVisible.value = true
                }
                return Offset.Zero
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (
                selectedList?.dataState is DataState.Data
                && selectedList.parentItems.lastOrNull()?.mediaType == MediaType.ARTIST
            ) {
                VerticalHidingContainer(
                    isVisible = isFabVisible.value,
                ) {
                    ExtendedFloatingActionButton(
                        modifier = modifier.navigationBarsPadding(),
                        onClick = { onShowAlbumsChange(!state.showAlbums) },
                        text = {
                            Text(
                                text = if (state.showAlbums) "Tracks" else "Albums",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        icon = {

                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Column(
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(scaffoldPadding)
                .consumeWindowInsets(scaffoldPadding)
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButton(
                    icon = FontAwesomeIcons.Solid.ArrowLeft,
                ) { onBack() }
                if (state.checkedItems.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "Media",
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    val artists =
                        state.checkedItems.filterIsInstance<AppMediaItem.Artist>().size
                    val albums = state.checkedItems.filterIsInstance<AppMediaItem.Album>().size
                    val tracks = state.checkedItems.filterIsInstance<AppMediaItem.Track>().size
                    val playlists =
                        state.checkedItems.filterIsInstance<AppMediaItem.Playlist>().size
                    val chosenItemsDescription = listOf(
                        Pair("artist", artists),
                        Pair("album", albums),
                        Pair("track", tracks),
                        Pair("playlist", playlists),
                    ).mapNotNull { pair ->
                        pair.takeIf { pair.second > 0 }
                            ?.let { "${it.second} ${it.first}${if (it.second > 1) "s" else ""}" }
                    }.joinToString(separator = ", ").capitalize(Locale.current)
                    ActionButton(
                        icon = TablerIcons.CircleDashed,
                        size = 24.dp,
                    ) { onCheckedItemsClear() }
                    Text(
                        modifier = Modifier.padding(start = 8.dp).weight(1f)
                            .basicMarquee(iterations = 100),
                        text = chosenItemsDescription,
                    )
                    ActionButton(
                        icon = TablerIcons.PlayerPlay,
                        size = 24.dp
                    ) { onPlaySelectedItems(QueueOption.PLAY) }
                    ActionButton(
                        icon = TablerIcons.PlayerTrackNext,
                        size = 24.dp
                    ) { onPlaySelectedItems(QueueOption.NEXT) }
                    ActionButton(
                        icon = TablerIcons.Plus,
                        size = 24.dp
                    ) { onPlaySelectedItems(QueueOption.ADD) }
                    ActionButton(
                        icon = TablerIcons.Replace,
                        size = 24.dp
                    ) { onPlaySelectedItems(QueueOption.REPLACE) }
                }
            }
            PrimaryTabRow(selectedTabIndex = state.libraryLists.indexOf(selectedList)) {
                state.libraryLists.forEach { list ->
                    Tab(
                        selected = list == selectedList,
                        onClick = {
                            onListSelected(list.tab)
                        },
                        text = {
                            Text(text = list.tab.name)
                        }
                    )
                }
            }
            selectedList?.let { list ->
                when (list.tab) {
                    LibraryViewModel.LibraryTab.Search -> {
                        if (list.parentItems.isEmpty()) {
                            SearchArea(
                                modifier = Modifier.padding(4.dp),
                                searchState = state.searchState,
                                onQueryChanged = onSearchQueryChanged,
                                onTypeChanged = onSearchTypeChanged,
                                onLibraryOnlyChanged = onSearchLibraryOnlyChanged,
                            )
                        }
                    }

                    LibraryViewModel.LibraryTab.Playlists -> {
                        if (list.parentItems.isEmpty()) {
                            NewPlaylistArea(
                                modifier = Modifier.padding(4.dp),
                                existingNames = (list.dataState as? DataState.Data)
                                    ?.data
                                    ?.map { it.name.trim() }
                                    ?.toSet()
                                    ?: emptySet(),
                                onCreatePlaylist = onCreatePlaylist
                            )
                        }
                    }

                    else -> Unit
                }
                ItemsListArea(
                    serverUrl = serverUrl,
                    list = list,
                    checkedItems = state.checkedItems,
                    ongoingItems = state.ongoingItems,
                    playlists = state.playlists,
                    nestedScrollConnection = nestedScrollConnection,
                    onItemClick = onItemClicked,
                    onCheckChanged = onCheckChanged,
                    onFavoriteChanged = onFavoriteChanged,
                    onAddToLibrary = onAddToLibrary,
                    onAddToPlaylist = onAddToPlaylist,
                    onUpClick = onUpClick,
                )
            }
        }
        ToastHost(
            toastState = toastState,
            modifier = Modifier
                .consumeWindowInsets(scaffoldPadding)
                .fillMaxSize()
                .padding(bottom = 48.dp)
        )
    }
}

@Composable
fun NewPlaylistArea(
    modifier: Modifier,
    existingNames: Set<String>,
    onCreatePlaylist: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    Row(
        modifier = modifier.fillMaxWidth().wrapContentHeight()
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = playlistName,
            onValueChange = { newText -> playlistName = newText },
            label = {
                Text(
                    text = "New playlist"
                )
            },
        )
        Button(
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
            enabled = playlistName.trim()
                .takeIf { it.isNotEmpty() && !existingNames.contains(playlistName) } != null,
            onClick = {
                onCreatePlaylist(playlistName)
                playlistName = ""
            }
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                imageVector = TablerIcons.Plus,
                contentDescription = "Add playlist"
            )
        }
    }
}

@Composable
private fun ItemsListArea(
    modifier: Modifier = Modifier,
    serverUrl: String?,
    list: LibraryViewModel.LibraryList,
    checkedItems: Set<AppMediaItem>,
    ongoingItems: List<AppMediaItem>,
    playlists: List<AppMediaItem.Playlist>,
    nestedScrollConnection: NestedScrollConnection,
    onItemClick: (LibraryViewModel.LibraryTab, AppMediaItem) -> Unit,
    onCheckChanged: (AppMediaItem) -> Unit,
    onFavoriteChanged: (AppMediaItem) -> Unit,
    onAddToLibrary: (AppMediaItem) -> Unit,
    onAddToPlaylist: (AppMediaItem.Track, AppMediaItem.Playlist) -> Unit,
    onUpClick: (LibraryViewModel.LibraryTab) -> Unit,
) {
    val parentItem = list.parentItems.lastOrNull()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        when (list.dataState) {
            is DataState.Loading -> {
                Text(modifier = Modifier.align(Alignment.Center), text = "Loading...")
            }

            is DataState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = "Error loading list"
                    )
                    parentItem?.let {
                        Button(onClick = { onUpClick(list.tab) }) {
                            Text(text = "BACK")
                        }
                    }
                }
            }

            is DataState.NoData -> {
                Text(modifier = Modifier.align(Alignment.Center), text = "Nothing to show")
            }

            is DataState.Data -> {
                if (list.dataState.data.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier.padding(bottom = 16.dp),
                            text = "No items found"
                        )
                        parentItem?.let {
                            Button(onClick = { onUpClick(list.tab) }) {
                                Text(text = "BACK")
                            }
                        }
                    }
                } else {
                    ItemsList(
                        parentItem = parentItem,
                        serverUrl = serverUrl,
                        items = list.dataState.data,
                        checkedItems = checkedItems,
                        ongoingItems = ongoingItems,
                        playlists = playlists,
                        nestedScrollConnection = nestedScrollConnection,
                        onClick = { item -> onItemClick(list.tab, item) },
                        onCheckChanged = onCheckChanged,
                        onFavoriteChanged = onFavoriteChanged,
                        onAddToLibrary = onAddToLibrary,
                        onAddToPlaylist = onAddToPlaylist,
                        onUpClick = { onUpClick(list.tab) },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchArea(
    modifier: Modifier,
    searchState: LibraryViewModel.SearchState,
    onQueryChanged: (String) -> Unit,
    onTypeChanged: (MediaType, Boolean) -> Unit,
    onLibraryOnlyChanged: (Boolean) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            searchState.mediaTypes.forEach { mediaType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            onTypeChanged(mediaType.type, !mediaType.isSelected)
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(26.dp),
                        imageVector =
                            if (mediaType.isSelected) TablerIcons.SquareCheck
                            else TablerIcons.Square,
                        contentDescription = "Select ${mediaType.type.name}"
                    )
                    Text(
                        text = mediaType.type.name.lowercase().capitalize(Locale.current),
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        onLibraryOnlyChanged(!searchState.libraryOnly)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(26.dp),
                    imageVector =
                        if (searchState.libraryOnly) TablerIcons.SquareCheck
                        else TablerIcons.Square,
                    contentDescription = "Toggle library only"
                )
                Text(
                    text = "In library",
                    modifier = Modifier.padding(end = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = searchState.query,
            onValueChange = { newText -> onQueryChanged(newText) },
            label = {
                Text(
                    text = if (searchState.query.trim().length < 3)
                        "Type at least 3 symbols for search"
                    else
                        "Search query"
                )
            },
        )
    }
}

@Composable
private fun ItemsList(
    modifier: Modifier = Modifier,
    serverUrl: String?,
    parentItem: AppMediaItem?,
    items: List<AppMediaItem>,
    ongoingItems: List<AppMediaItem>,
    checkedItems: Set<AppMediaItem>,
    playlists: List<AppMediaItem.Playlist>,
    nestedScrollConnection: NestedScrollConnection,
    onClick: (AppMediaItem) -> Unit,
    onCheckChanged: (AppMediaItem) -> Unit,
    onFavoriteChanged: (AppMediaItem) -> Unit,
    onAddToLibrary: (AppMediaItem) -> Unit,
    onAddToPlaylist: (AppMediaItem.Track, AppMediaItem.Playlist) -> Unit,
    onUpClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = modifier.fillMaxSize()
            .clip(shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSecondary)
            .nestedScroll(nestedScrollConnection)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        listState.scrollBy(-delta)
                    }
                },
            ),
        state = listState,
    ) {
        items(
            items = (parentItem?.let { listOf(it) } ?: emptyList()) + items
        ) { item ->
            val isChecked = item != parentItem && item in checkedItems
            val isOngoing = ongoingItems.any { it.hasAnyMappingFrom(item) }
            Row(
                modifier = Modifier
                    .alpha(if (isOngoing) 0.7f else 1f)
                    .padding(vertical = 1.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable(
                        onClick = { if (item == parentItem) onUpClick() else onClick(item) },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (item == parentItem) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 24.dp, start = 12.dp)
                            .size(18.dp),
                        imageVector = FontAwesomeIcons.Solid.ArrowUp,
                        contentDescription = "Up"
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(48.dp)
                ) {
                    val placeholder =
                        rememberPlaceholderPainter(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.MusicNote
                        )

                    AsyncImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(size = 4.dp)),
                        placeholder = placeholder,
                        fallback = placeholder,
                        model = item.imageInfo?.url(serverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .offset(x = 4.dp, y = 4.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color =
                                    if (isChecked) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(12.dp),
                            imageVector = when (item) {
                                is AppMediaItem.Track -> TablerIcons.FileMusic
                                is AppMediaItem.Album -> TablerIcons.Folder
                                is AppMediaItem.Artist -> TablerIcons.Man
                                is AppMediaItem.Playlist -> TablerIcons.List
                                else -> TablerIcons.QuestionMark
                            },
                            contentDescription = "Item type",
                            tint = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.wrapContentHeight().weight(1f)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color =
                            if (isChecked) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight =
                            if (item == parentItem || isChecked) FontWeight.Bold
                            else FontWeight.Normal
                    )
                    if (!item.isInLibrary) {
                        Text(
                            modifier = Modifier.fillMaxWidth().alpha(0.7f),
                            text = item.provider,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color =
                                if (isChecked) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight =
                                if (item == parentItem || isChecked) FontWeight.Bold
                                else FontWeight.Normal
                        )
                    }
                }
                if (checkedItems.isEmpty()) {
                    if (isOngoing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .size(18.dp)
                        )
                    } else {
                        if (item is AppMediaItem.Track) {
                            OverflowMenu(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .size(18.dp)
                                    .align(alignment = Alignment.CenterVertically),
                                icon = TablerIcons.Playlist,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                options = playlists.map { playlist ->
                                    OverflowMenuOption(
                                        title = playlist.name,
                                        onClick = {
                                            onAddToPlaylist(item, playlist)
                                        }
                                    )
                                }.ifEmpty {
                                    listOf(
                                        OverflowMenuOption(
                                            title = "No editable playlists",
                                            onClick = { /* No-op */ }
                                        )
                                    )
                                }
                            )
                        }
                        if (item.isInLibrary) {
                            Icon(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onFavoriteChanged(item) },
                                imageVector =
                                    if (item.favorite == true) FontAwesomeIcons.Solid.Heart
                                    else FontAwesomeIcons.Regular.Heart,
                                contentDescription = "Favorite item",
                                tint =
                                    if (item.favorite == true) Color(0xFFEF7BC4)
                                    else MaterialTheme.colorScheme.secondary,
                            )
                        } else {
                            Icon(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onAddToLibrary(item) },
                                imageVector = FontAwesomeIcons.Regular.Bookmark,
                                contentDescription = "Favorite item",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                if (item != parentItem && !isOngoing) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(26.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onCheckChanged(item) },
                        imageVector = if (isChecked) TablerIcons.SquareCheck else TablerIcons.Square,
                        contentDescription = "Select item",
                        tint = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Spacer(
                        Modifier
                            .padding(start = 8.dp)
                            .size(26.dp)
                    )
                }
            }
        }
    }
}