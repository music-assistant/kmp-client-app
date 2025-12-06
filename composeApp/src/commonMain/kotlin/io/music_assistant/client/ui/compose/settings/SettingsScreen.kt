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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import io.music_assistant.client.api.AuthState
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
        SessionState.Disconnected.Initial
    )
    val serverInfo = viewModel.serverInfo.collectAsStateWithLifecycle(null)
    val authState by viewModel.authState.collectAsStateWithLifecycle(AuthState.NotConnected)
    val loginError by viewModel.loginError.collectAsStateWithLifecycle(null)
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle(false)

    var shouldPopOnConnected by remember {
        mutableStateOf(
            sessionState !is SessionState.Connected
                    && sessionState != SessionState.Disconnected.Initial
        )
    }

    // Only pop back when connected AND authenticated (or auth not required)
    val isFullyConnected = sessionState is SessionState.Connected &&
            (authState is AuthState.Authenticated || authState is AuthState.NotRequired)

    if (isFullyConnected && shouldPopOnConnected) {
        navController.popBackStack()
    }

    BackHandler(enabled = true) {
        if (isFullyConnected) {
            navController.popBackStack()
        }
    }

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colors.background)
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
                if (isFullyConnected) {
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Connection settings
                ConnectionSettings(
                    sessionState = sessionState,
                    serverInfo = serverInfo.value,
                    connectionInfo = connectionInfo,
                    onConnect = { host, port, isTls ->
                        shouldPopOnConnected = true
                        viewModel.attemptConnection(host, port, isTls)
                    },
                    onDisconnect = { viewModel.disconnect() }
                )

                // Show authentication section when connected but auth is required
                if (sessionState is SessionState.Connected) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(24.dp))

                    AuthenticationSection(
                        authState = authState,
                        loginError = loginError,
                        isLoggingIn = isLoggingIn,
                        storedUsername = viewModel.getStoredUsername(),
                        isOAuthSupported = viewModel.isOAuthSupported(),
                        onAuthenticateWithToken = { token ->
                            viewModel.authenticateWithToken(token)
                        },
                        onLoginWithCredentials = { username, password ->
                            viewModel.loginWithCredentials(username, password)
                        },
                        onOAuthLogin = { viewModel.startOAuthLogin() },
                        onLogout = { viewModel.logout() },
                        onClearError = { viewModel.clearLoginError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionSettings(
    sessionState: SessionState,
    serverInfo: io.music_assistant.client.data.model.server.ServerInfo?,
    connectionInfo: io.music_assistant.client.api.ConnectionInfo?,
    onConnect: (String, String, Boolean) -> Unit,
    onDisconnect: () -> Unit
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
        textAlign = TextAlign.Center
    )

    Text(
        modifier = Modifier.padding(bottom = 24.dp),
        text = when (val state = sessionState) {
            is SessionState.Connected -> {
                "Connected to ${state.connectionInfo.host}:${state.connectionInfo.port}" +
                        (serverInfo?.let { "\nServer version ${it.serverVersion}, schema ${it.schemaVersion}" }
                            ?: "")
            }

            is SessionState.Connecting -> "Connecting to $ipAddress:$port."
            is SessionState.Disconnected -> {
                when (state) {
                    SessionState.Disconnected.ByUser -> ""
                    is SessionState.Disconnected.Error -> "Disconnected${state.reason?.message?.let { ": $it" } ?: ""}"
                    SessionState.Disconnected.Initial -> ""
                    SessionState.Disconnected.NoServerData -> "Please provide server address and port."
                    SessionState.Disconnected.AuthRequired -> "Authentication required."
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
        label = { Text("IP address") },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colors.onBackground,
        )
    )

    TextField(
        modifier = Modifier.padding(bottom = 16.dp),
        enabled = inputFieldsEnabled,
        value = port,
        onValueChange = { port = it },
        label = { Text("Port (8095 by default)") },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colors.onBackground,
        )
    )

    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = Modifier.align(Alignment.CenterVertically),
            enabled = inputFieldsEnabled,
            checked = isTls,
            onCheckedChange = { isTls = it }
        )
        Text(modifier = Modifier.align(Alignment.CenterVertically), text = "Use TLS")
    }

    Button(
        enabled = ipAddress.isValidHost() && port.isIpPort() && sessionState !is SessionState.Connecting,
        onClick = {
            if (sessionState is SessionState.Connected)
                onDisconnect()
            else
                onConnect(ipAddress, port, isTls)
        }
    ) {
        Text(
            text = if (sessionState is SessionState.Connected)
                "Disconnect"
            else
                "Connect"
        )
    }
}

@Composable
private fun AuthenticationSection(
    authState: AuthState,
    loginError: String?,
    isLoggingIn: Boolean,
    storedUsername: String?,
    isOAuthSupported: Boolean,
    onAuthenticateWithToken: (String) -> Unit,
    onLoginWithCredentials: (String, String) -> Unit,
    onOAuthLogin: () -> Unit,
    onLogout: () -> Unit,
    onClearError: () -> Unit
) {
    when (authState) {
        is AuthState.NotRequired -> {
            Text(
                text = "Authentication not required",
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.body1
            )
        }

        is AuthState.Authenticated -> {
            AuthenticatedView(
                user = authState.user,
                storedUsername = storedUsername,
                onLogout = onLogout
            )
        }

        is AuthState.Required, is AuthState.Failed -> {
            AuthenticationForm(
                authState = authState,
                loginError = loginError,
                isLoggingIn = isLoggingIn,
                isOAuthSupported = isOAuthSupported,
                onAuthenticateWithToken = onAuthenticateWithToken,
                onLoginWithCredentials = onLoginWithCredentials,
                onOAuthLogin = onOAuthLogin,
                onClearError = onClearError
            )
        }

        is AuthState.Authenticating -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticating...",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body1
                )
            }
        }

        AuthState.NotConnected -> {
            // Nothing to show
        }
    }
}

@Composable
private fun AuthenticatedView(
    user: io.music_assistant.client.api.User?,
    storedUsername: String?,
    onLogout: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Authenticated",
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.h5
        )

        val displayName = user?.displayName ?: user?.username ?: storedUsername
        if (displayName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Logged in as: $displayName",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1
            )
        }

        user?.role?.let { role ->
            Text(
                text = "Role: $role",
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.body2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onLogout) {
            Text("Logout")
        }
    }
}

@Composable
private fun AuthenticationForm(
    authState: AuthState,
    loginError: String?,
    isLoggingIn: Boolean,
    isOAuthSupported: Boolean,
    onAuthenticateWithToken: (String) -> Unit,
    onLoginWithCredentials: (String, String) -> Unit,
    onOAuthLogin: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Browser", "Token", "Login")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Authentication Required",
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Show error message
        loginError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (authState is AuthState.Failed) {
            Text(
                text = authState.reason,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.padding(horizontal = 32.dp),
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        onClearError()
                    },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> BrowserAuthForm(
                isOAuthSupported = isOAuthSupported,
                onOAuthLogin = onOAuthLogin
            )
            1 -> TokenAuthForm(
                isLoggingIn = isLoggingIn,
                onAuthenticate = onAuthenticateWithToken
            )
            2 -> CredentialsAuthForm(
                isLoggingIn = isLoggingIn,
                onLogin = onLoginWithCredentials
            )
        }
    }
}

@Composable
private fun BrowserAuthForm(
    isOAuthSupported: Boolean,
    onOAuthLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Login via your browser.\nThis will open the Music Assistant login page where you can sign in securely.",
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            enabled = isOAuthSupported,
            onClick = onOAuthLogin
        ) {
            Text("Open Login Page")
        }

        if (!isOAuthSupported) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Browser login is not supported on this platform.",
                color = MaterialTheme.colors.error.copy(alpha = 0.7f),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TokenAuthForm(
    isLoggingIn: Boolean,
    onAuthenticate: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Enter a long-lived access token.\nYou can create one in the Music Assistant web interface under Settings > Users.",
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Access Token") },
            singleLine = true,
            enabled = !isLoggingIn,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground,
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = token.isNotBlank() && !isLoggingIn,
            onClick = { onAuthenticate(token) }
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Authenticate")
            }
        }
    }
}

@Composable
private fun CredentialsAuthForm(
    isLoggingIn: Boolean,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Login with your Music Assistant credentials.",
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            enabled = !isLoggingIn,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoggingIn,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground,
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = username.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            onClick = { onLogin(username, password) }
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }
    }
}
