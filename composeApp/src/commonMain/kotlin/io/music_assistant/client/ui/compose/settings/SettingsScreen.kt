package io.music_assistant.client.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import io.music_assistant.client.ui.compose.common.ActionIcon
import io.music_assistant.client.ui.compose.nav.BackHandler
import io.music_assistant.client.ui.theme.ThemeSetting
import io.music_assistant.client.ui.theme.ThemeViewModel
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.isIpPort
import io.music_assistant.client.utils.isValidHost
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(navController: NavController) {
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
    val viewModel = koinViewModel<SettingsViewModel>()
    val connectionInfo by viewModel.connectionInfo.collectAsStateWithLifecycle(null)
    val sessionState by viewModel.connectionState.collectAsStateWithLifecycle(
        SessionState.Disconnected.Initial,
    )
    val serverInfo = viewModel.serverInfo.collectAsStateWithLifecycle(null)
    var shouldPopOnConnected by remember {
        mutableStateOf(
            sessionState !is SessionState.Connected &&
                sessionState != SessionState.Disconnected.Initial,
        )
    }
    if (sessionState is SessionState.Connected && shouldPopOnConnected) {
        navController.popBackStack()
    }
    BackHandler(enabled = true) {
        if (sessionState is SessionState.Connected) {
            navController.popBackStack()
        }
    }
    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Column(
            modifier =
                Modifier
                    .background(color = MaterialTheme.colors.background)
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .consumeWindowInsets(scaffoldPadding)
                    .systemBarsPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
            ) {
                if (sessionState is SessionState.Connected) {
                    ActionIcon(
                        icon = FontAwesomeIcons.Solid.ArrowLeft,
                    ) {
                        navController.popBackStack()
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                ThemeChooser(currentTheme = theme.value) { changedTheme ->
                    themeViewModel.switchTheme(changedTheme)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            var ipAddress by remember { mutableStateOf("") }
            var port by remember { mutableStateOf("8095") }
            var isTls by remember { mutableStateOf(false) }
            val inputFieldsEnabled = sessionState is SessionState.Disconnected
            LaunchedEffect(connectionInfo) {
                connectionInfo?.let {
                    ipAddress = it.host
                    port = it.port.toString()
                    isTls = it.isTls
                }
            }
            Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = "Server settings",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h3,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text =
                    when (val state = sessionState) {
                        is SessionState.Connected -> {
                            "Connected to ${state.connectionInfo.host}:${state.connectionInfo.port}" +
                                (
                                    serverInfo.value?.let { "\nServer version ${it.serverVersion}, schema ${it.schemaVersion}" }
                                        ?: ""
                                )
                        }

                        is SessionState.Connecting -> {
                            "Connecting to $ipAddress:$port."
                        }

                        is SessionState.Disconnected -> {
                            when (state) {
                                SessionState.Disconnected.ByUser -> ""
                                is SessionState.Disconnected.Error -> "Disconnected${state.reason?.message?.let { ": $it" } ?: ""}"
                                SessionState.Disconnected.Initial -> ""
                                SessionState.Disconnected.NoServerData -> "Please provide server address and port."
                            }
                        }
                    },
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                enabled = inputFieldsEnabled,
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = {
                    Text("IP address")
                },
                singleLine = true,
                colors =
                    TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onBackground,
                    ),
            )
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                enabled = inputFieldsEnabled,
                value = port,
                onValueChange = { port = it },
                label = {
                    Text("Port (8095 by default)")
                },
                singleLine = true,
                colors =
                    TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onBackground,
                    ),
            )
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    enabled = inputFieldsEnabled,
                    checked = isTls,
                    onCheckedChange = { isTls = it },
                )
                Text(modifier = Modifier.align(Alignment.CenterVertically), text = "Use TLS")
            }
            Button(
                enabled = ipAddress.isValidHost() && port.isIpPort() && sessionState !is SessionState.Connecting,
                onClick = {
                    if (sessionState is SessionState.Connected) {
                        viewModel.disconnect()
                    } else {
                        shouldPopOnConnected = true
                    }
                    viewModel.attemptConnection(ipAddress, port, isTls)
                },
            ) {
                Text(
                    text =
                        if (sessionState is SessionState.Connected) {
                            "Disconnect"
                        } else {
                            "Connect"
                        },
                )
            }
        }
    }
}
