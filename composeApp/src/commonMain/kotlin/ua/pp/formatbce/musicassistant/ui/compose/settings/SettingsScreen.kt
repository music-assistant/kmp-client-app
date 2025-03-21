package ua.pp.formatbce.musicassistant.ui.compose.settings

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
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ua.pp.formatbce.musicassistant.ui.compose.common.ActionIcon
import ua.pp.formatbce.musicassistant.ui.theme.ThemeSetting
import ua.pp.formatbce.musicassistant.ui.theme.ThemeViewModel
import ua.pp.formatbce.musicassistant.utils.ConnectionState
import ua.pp.formatbce.musicassistant.utils.isIpAddress
import ua.pp.formatbce.musicassistant.utils.isIpPort

class SettingsScreen : Screen {
    @OptIn(KoinExperimentalAPI::class, InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val themeViewModel = koinViewModel<ThemeViewModel>()
        val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<SettingsViewModel>()
        val connectionInfo = viewModel.connectionInfo.collectAsStateWithLifecycle(null)
        val connectionState = viewModel.connectionState.collectAsStateWithLifecycle(null)
        val serverInfo = viewModel.serverInfo.collectAsStateWithLifecycle(null)
        BackHandler(enabled = true) {
            if (connectionState.value is ConnectionState.Connected) {
                navigator.pop()
            }
        }
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .consumeWindowInsets(scaffoldPadding)
                    .systemBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    if (connectionState.value is ConnectionState.Connected) {
                        ActionIcon(
                            icon = FontAwesomeIcons.Solid.ArrowLeft,
                        ) {
                            navigator.pop()
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
                verticalArrangement = Arrangement.Center
            ) {
                var ipAddress by remember { mutableStateOf("") }
                var port by remember { mutableStateOf("8095") }
                val inputFieldsEnabled =
                    connectionState.value is ConnectionState.Disconnected
                            || connectionState.value is ConnectionState.NoServer
                LaunchedEffect(connectionInfo.value) {
                    connectionInfo.value?.let {
                        ipAddress = it.host
                        port = it.port.toString()
                    }
                }
                Text(
                    modifier = Modifier.padding(bottom = 24.dp),
                    text = "Server settings",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.h3,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.padding(bottom = 24.dp),
                    text = when (val state = connectionState.value) {
                        is ConnectionState.Connected -> {
                            "Connected to ${state.info.host}:${state.info.port}" +
                                    (serverInfo.value?.let { "\nServer version ${it.serverVersion}, schema ${it.schemaVersion}" }
                                        ?: "")
                        }

                        ConnectionState.Connecting -> "Connecting to $ipAddress:$port."
                        is ConnectionState.Disconnected -> "Disconnected${state.exception?.message?.let { ": $it" } ?: ""}"
                        ConnectionState.NoServer -> "Please provide server address and port."
                        null -> ""
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
                    maxLines = 1,
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onBackground,
                    )
                )
                TextField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    enabled = inputFieldsEnabled,
                    value = port,
                    onValueChange = { port = it },
                    label = {
                        Text("Port (8095 by default)")
                    },
                    maxLines = 1,
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onBackground,
                    )
                )
                Button(
                    enabled = ipAddress.isIpAddress() && port.isIpPort() && connectionState.value != ConnectionState.Connecting,
                    onClick = {
                        if (connectionState.value is ConnectionState.Connected)
                            viewModel.disconnect()
                        else
                            viewModel.attemptConnection(ipAddress, port)
                    }
                ) {
                    Text(
                        text = if (connectionState.value is ConnectionState.Connected)
                            "Disconnect"
                        else
                            "Connect"
                    )
                }
            }
        }
    }
}