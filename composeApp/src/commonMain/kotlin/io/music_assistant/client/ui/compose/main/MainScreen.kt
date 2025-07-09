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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Cog
import io.music_assistant.client.ui.compose.common.VerticalHidingContainer
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<MainViewModel>()
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(Unit) {
            viewModel.links.collectLatest { url -> uriHandler.openUri(url) }
        }
        val state by viewModel.state.collectAsStateWithLifecycle(MainViewModel.State.Loading)
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
            backgroundColor = MaterialTheme.colors.background,
            floatingActionButton = {
                VerticalHidingContainer(
                    isVisible = isFabVisible,
                ) {
                    Row(
                        modifier = Modifier.navigationBarsPadding().wrapContentSize(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (state is MainViewModel.State.Data) {
                            ExtendedFloatingActionButton(
                                modifier = Modifier.height(48.dp).padding(end = 8.dp),
                                onClick = {
                                    (state as? MainViewModel.State.Data)?.selectedPlayer
                                        ?.let { selected ->
                                            navigator.push(LibraryScreen(selected))
                                        }
                                },
                                text = {
                                    Text(
                                        text = "Library",
                                        style = MaterialTheme.typography.button
                                    )
                                }
                            )
                        }
                        FloatingActionButton(
                            modifier = Modifier.size(48.dp),
                            onClick = { navigator.push(SettingsScreen()) },
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
            var lastDataState by remember { mutableStateOf<MainViewModel.State.Data?>(null) }
            LaunchedEffect(state) {
                if (state is MainViewModel.State.NoServer) {
                    navigator.push(SettingsScreen())
                }
                if (state is MainViewModel.State.Data) {
                    lastDataState = state as MainViewModel.State.Data
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(scaffoldPadding)
                    .consumeWindowInsets(scaffoldPadding)
                    .systemBarsPadding()
                    .background(color = MaterialTheme.colors.background)
            ) {
                lastDataState?.let {
                    BoxWithConstraints {
                        val isLandscape = maxWidth > maxHeight
                        if (isLandscape) {
                            HorizontalDataLayout(
                                state = it,
                                nestedScrollConnection = nestedScrollConnection,
                                viewModel = viewModel,
                            )
                        } else {
                            VerticalDataLayout(
                                state = it,
                                nestedScrollConnection = nestedScrollConnection,
                                viewModel = viewModel,
                            )
                        }
                    }
                }
                when (state) {
                    MainViewModel.State.Disconnected,
                    MainViewModel.State.Loading,
                    MainViewModel.State.NoServer -> ServiceLayout(
                        stateValue = state,
                    )

                    is MainViewModel.State.Data -> Unit
                }
            }
        }
    }

    @Composable
    private fun ServiceLayout(
        stateValue: MainViewModel.State,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colors.background.copy(alpha = 0.9f))
                .clickable { /* Absorb interaction*/ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .padding(all = 16.dp),
                text = when (stateValue) {
                    is MainViewModel.State.Data -> "Connected to server"
                    MainViewModel.State.Disconnected -> "Disconnected"
                    MainViewModel.State.Loading -> "Connecting to server"
                    MainViewModel.State.NoServer -> "Please setup server connection"
                },
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground,
            )
            CircularProgressIndicator()
        }
    }


    @Composable
    private fun VerticalDataLayout(
        state: MainViewModel.State.Data,
        nestedScrollConnection: NestedScrollConnection,
        viewModel: MainViewModel,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playersData = state.playerData
            val selectedPlayerData = state.selectedPlayerData

            HorizontalPlayersPager(
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
            ) {
                viewModel.selectPlayer(it)
            }

            state.selectedPlayer?.let { playerData ->
                QueueSection(
                    modifier = Modifier.fillMaxSize(),
                    nestedScrollConnection = nestedScrollConnection,
                    playerData = playerData,
                    queueItems = selectedPlayerData?.queueItems,
                    chosenItemsIds = selectedPlayerData?.chosenItemsIds,
                    queueAction = { action ->
                        viewModel.queueAction(action)
                    },
                    onItemChosenChanged = { id ->
                        viewModel.onItemChosenChanged(
                            id
                        )
                    },
                    onChosenItemsClear = { viewModel.onChosenItemsClear() }
                )
            }
        }
    }

    @Composable
    private fun HorizontalDataLayout(
        state: MainViewModel.State.Data,
        nestedScrollConnection: NestedScrollConnection,
        viewModel: MainViewModel,
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playersData = state.playerData
            val selectedPlayerData = state.selectedPlayerData

            VerticalPlayersPager(
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
            ) {
                viewModel.selectPlayer(it)
            }

            state.selectedPlayer?.let { playerData ->
                QueueSection(
                    modifier = Modifier.fillMaxSize(),
                    nestedScrollConnection = nestedScrollConnection,
                    playerData = playerData,
                    queueItems = selectedPlayerData?.queueItems,
                    chosenItemsIds = selectedPlayerData?.chosenItemsIds,
                    queueAction = { action ->
                        viewModel.queueAction(action)
                    },
                    onItemChosenChanged = { id ->
                        viewModel.onItemChosenChanged(
                            id
                        )
                    },
                    onChosenItemsClear = { viewModel.onChosenItemsClear() }
                )
            }
        }
    }
}