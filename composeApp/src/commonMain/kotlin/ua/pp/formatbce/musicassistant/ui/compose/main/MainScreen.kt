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
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
        val state = viewModel.state.collectAsStateWithLifecycle(
            MainViewModel.State.Loading
        )
        val isFabVisible = rememberSaveable { mutableStateOf(true) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < -1) {
                        isFabVisible.value = false
                    } else if (available.y > 1) {
                        isFabVisible.value = true
                    }
                    return Offset.Zero
                }
            }
        }
        Scaffold(
            floatingActionButton = {
                if (state.value is MainViewModel.State.Data) {
                    Fab(
                        isVisible = isFabVisible.value,
                        text = "Library",
                        onClick = {
                            val data = state.value as? MainViewModel.State.Data
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
            val stateValue = state.value
            LaunchedEffect(state.value) {
                if (state.value is MainViewModel.State.NoServer) {
                    navigator.push(SettingsScreen())
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
                when (stateValue) {
                    MainViewModel.State.Disconnected,
                    MainViewModel.State.Loading,
                    MainViewModel.State.NoServer -> ServiceLayout(
                        stateValue = stateValue,
                        navigator = navigator
                    )

                    is MainViewModel.State.Data -> DataLayout(
                        state = stateValue,
                        nestedScrollConnection = nestedScrollConnection,
                        viewModel = viewModel,
                        navigator = navigator
                    )
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = when (stateValue) {
                    is MainViewModel.State.Data -> "Connected to server"
                    MainViewModel.State.Disconnected -> "Disconnected"
                    MainViewModel.State.Loading -> "Loading"
                    MainViewModel.State.NoServer -> "Please setup server connection"
                },
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground,
            )
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { navigator.push(SettingsScreen()) }
                    .size(48.dp),
                imageVector = FontAwesomeIcons.Solid.Cog,
                contentDescription = null,
                tint = MaterialTheme.colors.secondary,
            )
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