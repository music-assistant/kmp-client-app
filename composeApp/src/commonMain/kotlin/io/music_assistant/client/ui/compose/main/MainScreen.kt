package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Cog
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.ui.compose.common.VerticalHidingContainer
import io.music_assistant.client.utils.NavScreen
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(navigateTo: (NavScreen) -> Unit) {
    val viewModel = koinViewModel<MainViewModel>()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.links.collectLatest { url -> uriHandler.openUri(url) }
    }
    val state by viewModel.state.collectAsStateWithLifecycle(MainViewModel.PlayersState.Loading)
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < -1) {
                    isFabVisible = false
                } else if (available.y > 1) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            VerticalHidingContainer(
                isVisible = isFabVisible,
            ) {
                Row(
                    modifier = Modifier.navigationBarsPadding().wrapContentSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (state is MainViewModel.PlayersState.Data) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.height(48.dp).padding(end = 8.dp),
                            onClick = {navigateTo(NavScreen.Library(MediaType.ARTIST)) },
                            text = {
                                Text(
                                    text = "Media",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            icon = {

                            }
                        )
                    }
                    FloatingActionButton(
                        modifier = Modifier.size(48.dp),
                        onClick = { navigateTo(NavScreen.Settings) },
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = FontAwesomeIcons.Solid.Cog,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition = FabPosition.End,
    ) { scaffoldPadding ->
        var lastDataState by remember { mutableStateOf<MainViewModel.PlayersState.Data?>(null) }
        LaunchedEffect(state) {
            if (state is MainViewModel.PlayersState.NoServer || state is MainViewModel.PlayersState.NoAuth) {
                navigateTo(NavScreen.Settings)
            }
            if (state is MainViewModel.PlayersState.Data) {
                lastDataState = state as MainViewModel.PlayersState.Data
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scaffoldPadding)
                .consumeWindowInsets(scaffoldPadding)
                .systemBarsPadding()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            lastDataState?.let {
                BoxWithConstraints {
                    val isLandscape = maxWidth > maxHeight
                    if (isLandscape) {
                        HorizontalDataLayout(
                            serverUrl = serverUrl,
                            state = it,
                            nestedScrollConnection = nestedScrollConnection,
                            viewModel = viewModel,
                            forceShowFab = { isFabVisible = true },
                        )
                    } else {
                        VerticalDataLayout(
                            serverUrl = serverUrl,
                            state = it,
                            nestedScrollConnection = nestedScrollConnection,
                            viewModel = viewModel,
                            forceShowFab = { isFabVisible = true },
                        )
                    }
                }
            }
            when (state) {
                MainViewModel.PlayersState.Disconnected,
                MainViewModel.PlayersState.Loading,
                MainViewModel.PlayersState.NoServer,
                MainViewModel.PlayersState.NoAuth -> ServiceLayout(
                    stateValue = state,
                )

                is MainViewModel.PlayersState.Data -> Unit
            }
        }
    }
}

@Composable
private fun ServiceLayout(
    stateValue: MainViewModel.PlayersState,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            .clickable { /* Absorb interaction*/ },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(all = 16.dp),
            text = when (stateValue) {
                is MainViewModel.PlayersState.Data -> "Connected to server"
                MainViewModel.PlayersState.Disconnected -> "Disconnected"
                MainViewModel.PlayersState.Loading -> "Connecting to server"
                MainViewModel.PlayersState.NoServer -> "Please setup server connection"
                MainViewModel.PlayersState.NoAuth -> "Please authenticate with the server"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        CircularProgressIndicator()
    }
}


@Composable
private fun VerticalDataLayout(
    serverUrl: String?,
    state: MainViewModel.PlayersState.Data,
    nestedScrollConnection: NestedScrollConnection,
    viewModel: MainViewModel,
    forceShowFab: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val playersData = state.playerData
        val selectedPlayerData = state.selectedPlayer

        HorizontalPlayersPager(
            serverUrl = serverUrl,
            players = playersData,
            selectedPlayerId = selectedPlayerData?.playerId,
            playerAction = { playerData, action ->
                viewModel.playerAction(playerData, action)
            },
            settingsAction = { playerId ->
                viewModel.openPlayerSettings(playerId)
            },
            dspSettingsAction = { playerId ->
                viewModel.openPlayerDspSettings(playerId)
            },
            onListReordered = viewModel::onPlayersSortChanged,
            onSelected = { viewModel.selectPlayer(it) },
            forceShowFab = forceShowFab,
        )

        selectedPlayerData?.let { playerData ->
            QueueSection(
                modifier = Modifier.fillMaxSize(),
                nestedScrollConnection = nestedScrollConnection,
                serverUrl = serverUrl,
                players = playersData,
                playerData = playerData,
                queueItems = playerData.queueItems,
                chosenItemsIds = state.chosenIds,
                queueAction = { action ->
                    viewModel.queueAction(action)
                },
                onItemChosenChanged = { id ->
                    viewModel.onQueueItemChosenChanged(
                        id
                    )
                },
                onChosenItemsClear = { viewModel.onQueueChosenItemsClear() }
            )
        }
    }
}

@Composable
private fun HorizontalDataLayout(
    serverUrl: String?,
    state: MainViewModel.PlayersState.Data,
    nestedScrollConnection: NestedScrollConnection,
    viewModel: MainViewModel,
    forceShowFab: () -> Unit,
) {
    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val playersData = state.playerData
        val selectedPlayerData = state.selectedPlayer

        VerticalPlayersPager(
            serverUrl = serverUrl,
            players = playersData,
            selectedPlayerId = selectedPlayerData?.playerId,
            playerAction = { playerData, action ->
                viewModel.playerAction(playerData, action)
            },
            settingsAction = { playerId ->
                viewModel.openPlayerSettings(playerId)
            },
            dspSettingsAction = { playerId ->
                viewModel.openPlayerDspSettings(playerId)
            },
            onListReordered = viewModel::onPlayersSortChanged,
            onSelected = { viewModel.selectPlayer(it) },
            forceShowFab = forceShowFab,
        )

        selectedPlayerData?.let { playerData ->
            QueueSection(
                modifier = Modifier.fillMaxSize(),
                nestedScrollConnection = nestedScrollConnection,
                serverUrl = serverUrl,
                players = playersData,
                playerData = playerData,
                queueItems = playerData.queueItems,
                chosenItemsIds = state.chosenIds,
                queueAction = { action ->
                    viewModel.queueAction(action)
                },
                onItemChosenChanged = { id ->
                    viewModel.onQueueItemChosenChanged(
                        id
                    )
                },
                onChosenItemsClear = { viewModel.onQueueChosenItemsClear() }
            )
        }
    }
}