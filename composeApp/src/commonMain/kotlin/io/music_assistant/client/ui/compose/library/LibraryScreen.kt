package io.music_assistant.client.ui.compose.library

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.tablericons.CircleDashed
import compose.icons.tablericons.FileMusic
import compose.icons.tablericons.Folder
import compose.icons.tablericons.List
import compose.icons.tablericons.Man
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.PlayerTrackNext
import compose.icons.tablericons.Plus
import compose.icons.tablericons.QuestionMark
import compose.icons.tablericons.Replace
import compose.icons.tablericons.Square
import compose.icons.tablericons.SquareCheck
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.ui.compose.common.ActionIcon
import io.music_assistant.client.ui.compose.common.VerticalHidingContainer
import kotlinx.coroutines.launch

data class LibraryScreen(val playerData: PlayerData) : Screen {

    @OptIn(InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<LibraryViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle()
        val selectedList = state.value.libraryLists.firstOrNull { it.isSelected }
        BackHandler(enabled = true) {
            selectedList?.let {
                if (it.parentItems.isNotEmpty()) {
                    viewModel.onUpClick(it.tab)
                    return@BackHandler
                } else if (state.value.checkedItems.isNotEmpty()) {
                    viewModel.clearCheckedItems()
                    return@BackHandler
                }
            }
            navigator.pop()
        }
        Library(
            state = state.value,
            selectedList = selectedList,
            navigator = navigator,
            onListSelected = viewModel::onTabSelected,
            onItemClicked = viewModel::onItemClicked,
            onCheckChanged = viewModel::onItemCheckChanged,
            onCheckedItemsClear = viewModel::clearCheckedItems,
            onUpClick = viewModel::onUpClick,
            onShowAlbumsChange = viewModel::onShowAlbumsChange,
            onPlaySelectedItems = { option ->
                viewModel.playSelectedItems(playerData, option)
                navigator.pop()
            }
        )
    }

    @Composable
    private fun Library(
        modifier: Modifier = Modifier,
        state: LibraryViewModel.State,
        selectedList: LibraryViewModel.LibraryList?,
        navigator: Navigator,
        onListSelected: (LibraryViewModel.LibraryTab) -> Unit,
        onItemClicked: (LibraryViewModel.LibraryTab, AppMediaItem) -> Unit,
        onCheckChanged: (AppMediaItem) -> Unit,
        onCheckedItemsClear: () -> Unit,
        onUpClick: (LibraryViewModel.LibraryTab) -> Unit,
        onShowAlbumsChange: (Boolean) -> Unit,
        onPlaySelectedItems: (QueueOption) -> Unit,
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
            backgroundColor = MaterialTheme.colors.background,
            floatingActionButton = {
                if (
                    selectedList?.tab == LibraryViewModel.LibraryTab.Artists
                    && selectedList.listState is LibraryViewModel.ListState.Data
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
                                    style = MaterialTheme.typography.button
                                )
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
                    .background(color = MaterialTheme.colors.background)
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
                    ActionIcon(
                        icon = FontAwesomeIcons.Solid.ArrowLeft,
                    ) { navigator.pop() }
                    if (state.checkedItems.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = "Library",
                            style = MaterialTheme.typography.h6
                        )
                    } else {
                        val artists = state.checkedItems.filterIsInstance<AppMediaItem.Artist>().size
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
                        ActionIcon(
                            icon = TablerIcons.CircleDashed,
                            size = 24.dp,
                        ) { onCheckedItemsClear() }
                        Text(
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                                .basicMarquee(iterations = 100),
                            text = "$chosenItemsDescription, player: ${playerData.player.name}",
                        )
                        ActionIcon(
                            icon = TablerIcons.PlayerPlay,
                            size = 24.dp
                        ) { onPlaySelectedItems(QueueOption.PLAY) }
                        ActionIcon(
                            icon = TablerIcons.PlayerTrackNext,
                            size = 24.dp
                        ) { onPlaySelectedItems(QueueOption.NEXT) }
                        ActionIcon(
                            icon = TablerIcons.Plus,
                            size = 24.dp
                        ) { onPlaySelectedItems(QueueOption.ADD) }
                        ActionIcon(
                            icon = TablerIcons.Replace,
                            size = 24.dp
                        ) { onPlaySelectedItems(QueueOption.REPLACE) }
                    }
                }
                TabRow(selectedTabIndex = state.libraryLists.indexOf(selectedList)) {
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
                selectedList?.let {
                    ItemsListArea(
                        list = it,
                        checkedItems = state.checkedItems,
                        nestedScrollConnection = nestedScrollConnection,
                        onItemClick = onItemClicked,
                        onCheckChanged = onCheckChanged,
                        onUpClick = onUpClick,
                    )
                }
            }
        }
    }

    @Composable
    private fun ItemsListArea(
        modifier: Modifier = Modifier,
        list: LibraryViewModel.LibraryList,
        checkedItems: Set<AppMediaItem>,
        nestedScrollConnection: NestedScrollConnection,
        onItemClick: (LibraryViewModel.LibraryTab, AppMediaItem) -> Unit,
        onCheckChanged: (AppMediaItem) -> Unit,
        onUpClick: (LibraryViewModel.LibraryTab) -> Unit,
    ) {
        val parentItem = list.parentItems.lastOrNull()

        Box(
            modifier = modifier
                .fillMaxSize(),
        ) {
            when (list.listState) {
                LibraryViewModel.ListState.Loading -> {
                    Text(modifier = Modifier.align(Alignment.Center), text = "Loading...")
                }

                LibraryViewModel.ListState.Error -> {
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

                LibraryViewModel.ListState.NoData -> {
                    Text(modifier = Modifier.align(Alignment.Center), text = "No data")
                }

                is LibraryViewModel.ListState.Data -> {
                    if (list.listState.items.isEmpty()) {
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
                            items = list.listState.items,
                            checkedItems = checkedItems,
                            nestedScrollConnection = nestedScrollConnection,
                            onClick = { item -> onItemClick(list.tab, item) },
                            onCheckChanged = onCheckChanged,
                            onUpClick = { onUpClick(list.tab) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemsList(
        modifier: Modifier = Modifier,
        parentItem: AppMediaItem?,
        items: List<AppMediaItem>,
        checkedItems: Set<AppMediaItem>,
        nestedScrollConnection: NestedScrollConnection,
        onClick: (AppMediaItem) -> Unit,
        onCheckChanged: (AppMediaItem) -> Unit,
        onUpClick: () -> Unit,
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        LazyColumn(
            modifier = modifier.fillMaxSize()
                .clip(shape = RoundedCornerShape(16.dp))
                .background(MaterialTheme.colors.onSecondary)
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
            items((parentItem?.let { listOf(it) } ?: emptyList()) + items) { item ->
                val isChecked = item != parentItem && item in checkedItems
                Row(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(if (isChecked) MaterialTheme.colors.onPrimary else Color.Transparent)
                        .clickable(
                            onClick = { if (item == parentItem) onUpClick() else onClick(item) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (item == parentItem) {
                        Icon(
                            modifier = Modifier.padding(end = 24.dp).size(18.dp),
                            imageVector = FontAwesomeIcons.Solid.ArrowUp,
                            contentDescription = "Up"
                        )
                    }
                    Icon(
                        modifier = Modifier.padding(end = 16.dp).size(18.dp),
                        imageVector = when (item) {
                            is AppMediaItem.Track -> TablerIcons.FileMusic
                            is AppMediaItem.Album -> TablerIcons.Folder
                            is AppMediaItem.Artist -> TablerIcons.Man
                            is AppMediaItem.Playlist -> TablerIcons.List
                            else -> TablerIcons.QuestionMark
                        },
                        contentDescription = "Item icon",
                        tint = if (isChecked) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isChecked) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.body1,
                        fontWeight = if (item == parentItem || isChecked) FontWeight.Bold else FontWeight.Normal
                    )
                    if (item != parentItem) {
                        Icon(
                            modifier = Modifier.padding(start = 16.dp).size(18.dp)
                                .clickable { onCheckChanged(item) },
                            imageVector = if (isChecked) TablerIcons.SquareCheck else TablerIcons.Square,
                            contentDescription = "Check",
                            tint = if (isChecked) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
                        )
                    }
                }
            }
        }
    }
}