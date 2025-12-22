@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.PlayerControls
import io.music_assistant.client.utils.NavScreen
import io.music_assistant.client.utils.conditional
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel(),
    navigateTo: (NavScreen) -> Unit
) {
    var showPlayersView by remember { mutableStateOf(false) }

    val recommendationsState = viewModel.recommendationsState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val playersState by viewModel.playersState.collectAsStateWithLifecycle()
    // Single pager state used across all views
    val data = playersState as? HomeScreenViewModel.PlayersState.Data
    val playerPagerState = rememberPagerState(
        initialPage = data?.selectedPlayerIndex ?: 0,
        pageCount = { data?.playerData?.size ?: 0 }
    )

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
                        letterSpacing = 2.sp
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
                                    textAlign = TextAlign.Center
                                )

                                is HomeScreenViewModel.PlayersState.Data -> {
                                    if (state.playerData.isEmpty()) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            text = "No players available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        PlayersPager(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                            playerPagerState = playerPagerState,
                                            playersState = state,
                                            playerAction = { playerData, action ->
                                                viewModel.playerAction(playerData, action)
                                            },
                                            showQueue = false,
                                        )
                                    }
                                }

                                else -> Text(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    text = "No players available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
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
                            textAlign = TextAlign.Center
                        )

                        is HomeScreenViewModel.PlayersState.Data -> {
                            if (state.playerData.isEmpty()) {
                                Text(
                                    modifier = Modifier.fillMaxSize(),
                                    text = "No players available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                PlayersView(
                                    onCollapse = { showPlayersView = false },
                                    playerPagerState = playerPagerState,
                                    playersState = state,
                                    playerAction = { playerData, action ->
                                        viewModel.playerAction(playerData, action)
                                    },
                                )
                            }
                        }

                        else -> Text(
                            modifier = Modifier.fillMaxSize(),
                            text = "No players available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
    playerAction: (PlayerData, PlayerAction) -> Unit,
    showQueue: Boolean = true
) {
    var isQueueExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        PageIndicator(playerPagerState)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(
                    visible = isQueueExpanded.takeIf { showQueue } != false,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                ) {
                    CompactPlayerItem(item = player)
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
                            playerAction = playerAction,
                        )
                    }
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))

                player.queue.takeIf { showQueue }?.let { queue ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .conditional(
                                condition = isQueueExpanded,
                                ifTrue = { weight(1f) },
                                ifFalse = { wrapContentHeight() }
                            )
                            .animateContentSize()  // Add animation here too
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { isQueueExpanded = !isQueueExpanded })
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isQueueExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = "Toggle Queue"
                            )
                        }

                        if (isQueueExpanded) {
                            when (queue) {
                                is DataState.Error -> Text(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    text = "Error loading",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                is DataState.Loading -> Text(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    text = "Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                is DataState.NoData -> Text(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    text = "No items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                is DataState.Data -> {
                                    when (val items = queue.data.items) {
                                        is DataState.Error -> Text(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            text = "Error loading",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )

                                        is DataState.Loading -> Text(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            text = "Loading...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )

                                        is DataState.NoData -> Text(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            text = "Not loaded",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )

                                        is DataState.Data -> {
                                            if (items.data.isEmpty()) {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                                    text = "No items",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    items(items.data.size) { index ->
                                                        QueueItemRow(
                                                            items.data[index],
                                                            index + 1
                                                        )
                                                    }
                                                }
                                            }

                                        }
                                    }

                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(0.5f).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            modifier = Modifier.weight(0.5f),
                            value = 0.4f,
                            onValueChange = { /* TODO */ },
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun PageIndicator(
    playerPagerState: PagerState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(playerPagerState.pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == playerPagerState.currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == playerPagerState.currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.3f
                            )
                    )
            )
        }
    }
}

@Composable
fun CompactPlayerItem(item: PlayerData) {
    val track = item.queueInfo?.currentItem?.track
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album cover on the far left
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Load actual album image
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "Album",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track?.name ?: "(idle)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = track?.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
            }
        }
    }
}

@Composable
private fun FullPlayerItem(
    modifier: Modifier,
    item: PlayerData,
    playerAction: (PlayerData, PlayerAction) -> Unit
) {
    val track = item.queueInfo?.currentItem?.track
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {


        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = "Album",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )


        // Track info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track?.name ?: "(idle)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = track?.subtitle ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val currentProgress = track?.duration
            ?.let { (item.queueInfo.elapsedTime?.toFloat() ?: 0f) / it.toFloat() } ?: 0f

        // Progress bar
        Slider(
            value = currentProgress,
            onValueChange = { /* TODO seek */ },
            modifier = Modifier.fillMaxWidth(),
            thumb = {},
        )

        PlayerControls(
            playerData = item,
            playerAction = playerAction,
            enabled = !item.player.isAnnouncing
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayersView(
    onCollapse: () -> Unit,
    playerPagerState: PagerState,
    playersState: HomeScreenViewModel.PlayersState.Data,
    playerAction: (PlayerData, PlayerAction) -> Unit
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
            playerAction = playerAction,
        )
    }
}

@Composable
fun QueueItemRow(item: QueueTrack, position: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { /* TODO */ }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.track.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}