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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import io.music_assistant.client.ui.compose.common.ActionIcon
import io.music_assistant.client.ui.compose.nav.BackHandler
import io.music_assistant.client.ui.theme.ThemeSetting
import io.music_assistant.client.ui.theme.ThemeViewModel
import io.music_assistant.client.utils.AuthProcessState
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.isIpPort
import io.music_assistant.client.utils.isValidHost
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
    val viewModel = koinViewModel<SettingsViewModel>()
    val savedConnectionInfo by viewModel.savedConnectionInfo.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    var shouldPopOnAuth by remember { mutableStateOf(false) }
    if ((sessionState as? SessionState.Connected)?.dataConnectionState != DataConnectionState.Ready) {
        shouldPopOnAuth = true
    } else if (shouldPopOnAuth) {
        onBack()
    }

    BackHandler(enabled = true) {
        if (sessionState is SessionState.Connected) {
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(scaffoldPadding)
                .consumeWindowInsets(scaffoldPadding)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            ) {
                if ((sessionState as? SessionState.Connected)?.dataConnectionState is DataConnectionState.Ready) {
                    ActionIcon(
                        icon = FontAwesomeIcons.Solid.ArrowLeft,
                        size = 24.dp
                    ) {
                        onBack()
                    }
                } else {
                    Spacer(Modifier.size(24.dp))
                }
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .align(Alignment.CenterVertically),
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
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
            var isTls by remember { mutableStateOf(false) }
            val serverInputsFieldVisible = sessionState is SessionState.Disconnected
            LaunchedEffect(savedConnectionInfo) {
                savedConnectionInfo?.let {
                    ipAddress = it.host
                    port = it.port.toString()
                    isTls = it.isTls
                }
            }
            Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = when (val state = sessionState) {
                    is SessionState.Connected -> {
                        savedConnectionInfo?.let { conn ->
                            "Connected to ${conn.host}:${conn.port}" +
                                    (state.serverInfo?.let { server -> "\nServer version ${server.serverVersion}, schema ${server.schemaVersion}" }
                                        ?: "")
                        } ?: "Unknown connection"
                    }

                    SessionState.Connecting -> "Connecting to $ipAddress:$port."
                    is SessionState.Disconnected -> {
                        when (state) {
                            SessionState.Disconnected.ByUser -> ""
                            is SessionState.Disconnected.Error -> "Disconnected${state.reason?.message?.let { ": $it" } ?: ""}"
                            SessionState.Disconnected.Initial -> ""
                            SessionState.Disconnected.NoServerData -> "Please provide server address and port."
                        }
                    }
                },
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            if (serverInputsFieldVisible) {
                TextField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = {
                        Text("IP address")
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
                TextField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    value = port,
                    onValueChange = { port = it },
                    label = {
                        Text("Port (8095 by default)")
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        checked = isTls,
                        onCheckedChange = { isTls = it })
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = "Use TLS"
                    )
                }
            }
            Button(
                enabled = ipAddress.isValidHost() && port.isIpPort() && sessionState != SessionState.Connecting,
                onClick = {
                    if (sessionState is SessionState.Connected)
                        viewModel.disconnect()
                    else
                        viewModel.attemptConnection(ipAddress, port, isTls)
                }
            ) {
                Text(
                    text = if (sessionState is SessionState.Connected)
                        "Disconnect"
                    else
                        "Connect"
                )
            }
            AuthSection(
                state = sessionState,
                onLoginClick = viewModel::login,
                onLogoutClick = viewModel::logout
            )
        }
    }
}

@Composable
fun AuthSection(
    modifier: Modifier = Modifier,
    state: SessionState,
    onLoginClick: (String, String) -> Unit,
    onLogoutClick: () -> Unit,
) {
    if (state !is SessionState.Connected) {
        return
    }
    when (val connState = state.dataConnectionState) {
        is DataConnectionState.AwaitingAuth -> {
            val fieldsEnabled = connState.authProcessState != AuthProcessState.InProgress
            var login by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isPasswordVisible by remember { mutableStateOf(false) }
            val error = when (connState.authProcessState) {
                is AuthProcessState.Failed -> connState.authProcessState.reason
                AuthProcessState.LoggedOut -> "Logged out"
                else -> null
            }
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    enabled = fieldsEnabled,
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Login") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
                TextField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    enabled = fieldsEnabled,
                    value = password,
                    visualTransformation =
                        if (isPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    trailingIcon = {
                        val icon =
                            if (isPasswordVisible)
                                Icons.Filled.VisibilityOff
                            else
                                Icons.Filled.Visibility
                        val description =
                            if (isPasswordVisible)
                                "Hide password"
                            else
                                "Show password"
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = description)
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
                error?.let {
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    enabled = fieldsEnabled && login.isNotEmpty() && password.isNotEmpty(),
                    onClick = {
                        onLoginClick(login, password)
                    }
                ) {
                    Text(text = "Login")
                }
            }
        }

        DataConnectionState.Ready -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = modifier.padding(top = 32.dp),
                    text = "Logged in as ${state.user?.description ?: "Unknown User"}",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { onLogoutClick() }
                ) {
                    Text(text = "Logout")
                }
            }
        }

        DataConnectionState.AwaitingServerInfo -> Unit
    }
}