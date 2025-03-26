package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FabPosition
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Cog
import ua.pp.formatbce.musicassistant.ui.compose.common.Fab
import ua.pp.formatbce.musicassistant.ui.compose.library.LibraryScreen
import ua.pp.formatbce.musicassistant.ui.compose.settings.SettingsScreen

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<MainViewModel>()
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
            floatingActionButton = {
                if (state is MainViewModel.State.Data) {
                    Fab(
                        isVisible = isFabVisible,
                        text = "Library",
                        onClick = {
                            val data = state as? MainViewModel.State.Data
                            data?.playerData?.firstOrNull { it.player.id == data.selectedPlayerData?.playerId }
                                ?.let { selected ->
                                    navigator.push(LibraryScreen(selected))
                                }
                        })
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
                    DataLayout(
                        state = it,
                        nestedScrollConnection = nestedScrollConnection,
                        viewModel = viewModel,
                        navigator = navigator
                    )
                }
                when (state) {
                    MainViewModel.State.Disconnected,
                    MainViewModel.State.Loading,
                    MainViewModel.State.NoServer -> ServiceLayout(
                        stateValue = state,
                        navigator = navigator
                    )

                    is MainViewModel.State.Data -> Unit
                }
            }
        }
    }

    @Composable
    private fun ServiceLayout(
        stateValue: MainViewModel.State,
        navigator: Navigator
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colors.background.copy(alpha = 0.9f))
                .clickable() { /* Absorb interaction*/ },
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
            Button(
                modifier = Modifier
                    .padding(all = 16.dp),
                onClick = { navigator.push(SettingsScreen()) }
            ) {
                Text(text = "Settings")
            }
        }
    }


    @Composable
    private fun DataLayout(
        state: MainViewModel.State.Data,
        nestedScrollConnection: NestedScrollConnection,
        viewModel: MainViewModel,
        navigator: Navigator
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playersData = state.playerData
            val selectedPlayerData = state.selectedPlayerData
            PlayersRow(
                modifier = Modifier.padding(top = 8.dp),
                players = playersData,
                selectedPlayerId = selectedPlayerData?.playerId,
                playerAction = { playerData, action ->
                    viewModel.playerAction(playerData, action)
                },
                onListReordered = viewModel::onPlayersSortChanged,
            ) { viewModel.selectPlayer(it) }

            playersData
                .firstOrNull { it.player.id == selectedPlayerData?.playerId }
                ?.let { playerData ->
                    PlayerDetails(
                        modifier = Modifier.fillMaxWidth().weight(1f),
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
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { navigator.push(SettingsScreen()) }
                    .size(24.dp),
                imageVector = FontAwesomeIcons.Solid.Cog,
                contentDescription = null,
                tint = MaterialTheme.colors.secondary,
            )
        }
    }
}