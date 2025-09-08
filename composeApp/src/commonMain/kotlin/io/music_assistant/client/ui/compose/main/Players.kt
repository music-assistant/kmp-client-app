package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.PlayerData
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HorizontalPlayersPager(
    modifier: Modifier = Modifier,
    serverUrl: String?,
    players: List<PlayerData> = emptyList(),
    selectedPlayerId: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
    onListReordered: (List<String>) -> Unit,
    onSelected: (Player) -> Unit,
) {
    val pagerState = key(players.map { it.player.id }) {
        rememberPagerState(
            initialPage = players.indexOfFirst { it.player.id == selectedPlayerId }
                .takeIf { it >= 0 } ?: 0,
            pageCount = { players.size },
        )
    }

    fun onItemMoved(indexShift: Int) {
        val newPlayers = players.map { it.player.id }.toMutableList().apply {
            val newIndex = (pagerState.currentPage + indexShift).coerceIn(0, players.size - 1)
            add(newIndex, removeAt(pagerState.currentPage))
        }
        onListReordered(newPlayers)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelected(players[page].player)
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            state = pagerState,
        ) { page ->
            val playerData = players[page]
            val player = playerData.player
            PlayerCard(
                modifier = Modifier.padding(all = 8.dp).fillMaxWidth().height(180.dp),
                serverUrl = serverUrl,
                playerData = playerData,
                isSelected = selectedPlayerId == player.id,
                playerAction = playerAction,
                settingsAction = settingsAction,
                dspSettingsAction = dspSettingsAction,
            )
        }
        HorizontalPagerIndicator(
            modifier = Modifier,
            pagerState = pagerState,
            onItemMoved = ::onItemMoved,
        )
    }
}

@Composable
fun VerticalPlayersPager(
    modifier: Modifier = Modifier,
    serverUrl: String?,
    players: List<PlayerData> = emptyList(),
    selectedPlayerId: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
    onListReordered: (List<String>) -> Unit,
    onSelected: (Player) -> Unit,
) {
    val pagerState = key(players.map { it.player.id }) {
        rememberPagerState(
            initialPage = players.indexOfFirst { it.player.id == selectedPlayerId }
                .takeIf { it >= 0 } ?: 0,
            pageCount = { players.size },
        )
    }

    fun onItemMoved(indexShift: Int) {
        val newPlayers = players.map { it.player.id }.toMutableList().apply {
            val newIndex = (pagerState.currentPage + indexShift).coerceIn(0, players.size - 1)
            add(newIndex, removeAt(pagerState.currentPage))
        }
        onListReordered(newPlayers)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelected(players[page].player)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(fraction = 0.5f).fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerticalPager(
            modifier = Modifier.fillMaxHeight().weight(1f),
            state = pagerState,
        ) { page ->
            val playerData = players[page]
            val player = playerData.player
            PlayerCard(
                modifier = Modifier.padding(all = 8.dp).fillMaxSize(),
                serverUrl = serverUrl,
                playerData = playerData,
                isSelected = selectedPlayerId == player.id,
                playerAction = playerAction,
                settingsAction = settingsAction,
                dspSettingsAction = dspSettingsAction,
            )
        }
        VerticalPagerIndicator(
            modifier = Modifier,
            pagerState = pagerState,
            onItemMoved = ::onItemMoved,
        )
    }
}

@Preview
@Composable
fun HorizontalPlayersPagerPreview() {
    HorizontalPlayersPager(
        serverUrl = null,
        players = listOf(
            PlayerData(
                Player(
                    "1",
                    "Player 1",
                    shouldBeShown = true,
                    canSetVolume = true,
                    queueId = null,
                    isPlaying = false,
                    isBuiltin = false,
                    isAnnouncing = false
                ), null
            ),
            PlayerData(
                Player(
                    "2",
                    "Player 2",
                    shouldBeShown = true,
                    canSetVolume = true,
                    queueId = null,
                    isPlaying = false,
                    isBuiltin = false,
                    isAnnouncing = false
                ), null
            ),
            PlayerData(
                Player(
                    "3",
                    "Player 3",
                    shouldBeShown = true,
                    canSetVolume = true,
                    queueId = null,
                    isPlaying = false,
                    isBuiltin = false,
                    isAnnouncing = false
                ), null
            ),
        ),
        selectedPlayerId = "2",
        playerAction = { _, _ -> },
        settingsAction = {},
        dspSettingsAction = {},
        onListReordered = {},
        onSelected = {}
    )
}

@Preview
@Composable
fun VerticalPlayersPagerPreview() {
    Box(modifier = Modifier.size(1920.dp)) {
        VerticalPlayersPager(
            serverUrl = null,
            players = listOf(
                PlayerData(
                    Player(
                        "1",
                        "Player 1",
                        shouldBeShown = true,
                        canSetVolume = true,
                        queueId = null,
                        isPlaying = false,
                        isBuiltin = false,
                        isAnnouncing = false
                    ), null
                ),
                PlayerData(
                    Player(
                        "2",
                        "Player 2",
                        shouldBeShown = true,
                        canSetVolume = true,
                        queueId = null,
                        isPlaying = false,
                        isBuiltin = false,
                        isAnnouncing = false
                    ), null
                ),
                PlayerData(
                    Player(
                        "3",
                        "Player 3",
                        shouldBeShown = true,
                        canSetVolume = true,
                        queueId = null,
                        isPlaying = false,
                        isBuiltin = false,
                        isAnnouncing = false
                    ), null
                ),
            ),
            selectedPlayerId = "2",
            playerAction = { _, _ -> },
            settingsAction = {},
            dspSettingsAction = {},
            onListReordered = {},
            onSelected = {}
        )
    }
}
