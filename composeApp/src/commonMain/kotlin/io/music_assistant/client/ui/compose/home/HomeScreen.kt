@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.ui.compose.common.HorizontalPagerIndicator
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.nav.BackHandler
import io.music_assistant.client.utils.NavScreen
import io.music_assistant.client.utils.conditional
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel(),
    navigateTo: (NavScreen) -> Unit,
) {
    var showPlayersView by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val recommendationsState = viewModel.recommendationsState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val playersState by viewModel.playersState.collectAsStateWithLifecycle()
    // Single pager state used across all views
    val data = playersState as? HomeScreenViewModel.PlayersState.Data
    val playerPagerState = rememberPagerState(
        initialPage = data?.selectedPlayerIndex ?: 0,
        pageCount = { data?.playerData?.size ?: 0 }
    )

    // Only handle back when player view is shown to collapse it
    // When disabled, system handles back press to close app
    BackHandler(enabled = showPlayersView) {
        showPlayersView = false
    }

    // Update selected player when pager changes to load queue items
    LaunchedEffect(playerPagerState, playersState) {
        snapshotFlow { playerPagerState.currentPage }.collect { currentPage ->
            data?.playerData?.getOrNull(currentPage)?.let { playerData ->
                viewModel.selectPlayer(playerData.player)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TODO: Replace with actual logo
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MASSIVE",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { paddingValues ->
        val connectionState = recommendationsState.value.connectionState
        val dataState = recommendationsState.value.recommendations
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Simple slide transition between main screen and big player
            AnimatedContent(
                targetState = showPlayersView,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { if (targetState) it else -it },
                        animationSpec = tween(300)
                    ) togetherWith slideOutVertically(
                        targetOffsetY = { if (targetState) -it else it },
                        animationSpec = tween(300)
                    )
                },
                label = "player_transition"
            ) { isPlayersViewShown ->
                if (!isPlayersViewShown) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {

                        LandingPage(
                            Modifier.weight(1f),
                            connectionState,
                            dataState,
                            serverUrl,
                            onItemClick = viewModel::onRecommendationItemClicked,
                            onLongItemClick = { item ->  /*TODO*/ },
                            onRowActionClick = { id -> viewModel.onRowButtonClicked(id) },
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clickable { showPlayersView = true }
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(top = 8.dp),
                        ) {
                            when (val state = playersState) {
                                is HomeScreenViewModel.PlayersState.Loading -> Text(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    text = "Loading players...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                is HomeScreenViewModel.PlayersState.Data -> {
                                    if (state.playerData.isEmpty()) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            text = "No players available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        PlayersPager(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                            playerPagerState = playerPagerState,
                                            playersState = state,
                                            serverUrl = serverUrl,
                                            playerAction = { playerData, action ->
                                                viewModel.playerAction(playerData, action)
                                            },
                                            showQueue = false,
                                            onItemMoved = null
                                        )
                                    }
                                }

                                else -> Text(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    text = "No players available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    when (val state = playersState) {
                        is HomeScreenViewModel.PlayersState.Loading -> Text(
                            modifier = Modifier.fillMaxSize(),
                            text = "Loading players...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        is HomeScreenViewModel.PlayersState.Data -> {
                            if (state.playerData.isEmpty()) {
                                Text(
                                    modifier = Modifier.fillMaxSize(),
                                    text = "No players available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                PlayersView(
                                    onCollapse = { showPlayersView = false },
                                    playerPagerState = playerPagerState,
                                    playersState = state,
                                    serverUrl = serverUrl,
                                    playerAction = { playerData, action ->
                                        viewModel.playerAction(playerData, action)
                                    },
                                    onItemMoved = { indexShift ->
                                        val newIndex =
                                            (playerPagerState.currentPage + indexShift).coerceIn(
                                                0,
                                                state.playerData.size - 1
                                            )
                                        val newPlayers =
                                            state.playerData.map { it.player.id }.toMutableList()
                                                .apply {
                                                    add(
                                                        newIndex,
                                                        removeAt(playerPagerState.currentPage)
                                                    )
                                                }
                                        viewModel.onPlayersSortChanged(newPlayers)
                                        coroutineScope.launch {
                                            playerPagerState.animateScrollToPage(newIndex)
                                        }
                                    }
                                )
                            }
                        }

                        else -> Text(
                            modifier = Modifier.fillMaxSize(),
                            text = "No players available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayersPager(
    modifier: Modifier = Modifier,
    playerPagerState: PagerState,
    playersState: HomeScreenViewModel.PlayersState.Data,
    serverUrl: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    showQueue: Boolean = true,
    onItemMoved: ((Int) -> Unit)?,
) {
    var isQueueExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        HorizontalPagerIndicator(
            pagerState = playerPagerState,
            onItemMoved = onItemMoved
        )
        HorizontalPager(
            modifier = Modifier.wrapContentHeight(),
            state = playerPagerState
        ) { page ->

            val player = playersState.playerData.getOrNull(page) ?: return@HorizontalPager

            Column {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    text = player.player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(
                    visible = isQueueExpanded.takeIf { showQueue } != false,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                ) {
                    if (showQueue) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize()
                                .clickable { isQueueExpanded = !isQueueExpanded }) {
                            CompactPlayerItem(
                                item = player,
                                serverUrl = serverUrl,
                                playerAction = playerAction
                            )
                        }
                    } else {
                        CompactPlayerItem(
                            item = player,
                            serverUrl = serverUrl,
                            playerAction = playerAction
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .conditional(
                            condition = isQueueExpanded.takeIf { showQueue } == false,
                            ifTrue = { weight(1f) },
                            ifFalse = { wrapContentHeight() }
                        )
                ) {

                    AnimatedVisibility(
                        visible = isQueueExpanded.takeIf { showQueue } == false,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        FullPlayerItem(
                            modifier = Modifier.fillMaxSize(),
                            item = player,
                            serverUrl = serverUrl,
                            playerAction = playerAction,
                        )
                    }
                }

                if (showQueue && player.player.canSetVolume && player.player.volumeLevel != null) {
                    var currentVolume by remember(player.player.volumeLevel) {
                        mutableStateOf(player.player.volumeLevel)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            modifier = Modifier.weight(1f),
                            value = currentVolume,
                            valueRange = 0f..100f,
                            onValueChange = {
                                currentVolume = it
                                playerAction(
                                    player,
                                    PlayerAction.VolumeSet(it.toDouble())
                                )
                            },
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { MutableInteractionSource() },
                                    thumbSize = androidx.compose.ui.unit.DpSize(16.dp, 16.dp)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    thumbTrackGapSize = 0.dp,
                                    trackInsideCornerSize = 0.dp,
                                    drawStopIndicator = null,
                                    modifier = Modifier.height(4.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))

                player.queue.takeIf { showQueue }?.let { queue ->
                    CollapsibleQueue(
                        modifier = Modifier
                            .conditional(
                                condition = isQueueExpanded,
                                ifTrue = { weight(1f) },
                                ifFalse = { wrapContentHeight() }
                            ),
                        queue = queue,
                        isQueueExpanded = isQueueExpanded,
                        onQueueExpandedSwitch = { isQueueExpanded = !isQueueExpanded }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayersView(
    onCollapse: () -> Unit,
    playerPagerState: PagerState,
    playersState: HomeScreenViewModel.PlayersState.Data,
    serverUrl: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onItemMoved: ((Int) -> Unit)
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        // Close button
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.ExpandMore, "Collapse", modifier = Modifier.size(32.dp))
        }
        PlayersPager(
            modifier = Modifier.fillMaxSize(),
            playerPagerState = playerPagerState,
            playersState = playersState,
            serverUrl = serverUrl,
            playerAction = playerAction,
            onItemMoved = onItemMoved,
        )
    }
}

@Composable
fun QueueItemRow(
    item: QueueTrack,
    position: Int,
    isCurrentItem: Boolean,
    isPlayedItem: Boolean
) {
    val alpha = if (isPlayedItem) 0.4f else 1f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !isCurrentItem) { /* TODO */ }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show play icon for current item, number for others
            if (isCurrentItem) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Currently playing",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    modifier = Modifier.width(16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentItem) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.track.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}