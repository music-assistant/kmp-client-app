package io.music_assistant.client.ui.compose.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import io.music_assistant.client.auth.AuthState
import io.music_assistant.client.data.model.server.User
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthenticationPanel(
    viewModel: AuthenticationViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    user: User?
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var loginError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        Logger.e("State $authState")
        if (authState is AuthState.Error) {
            loginError = (authState as? AuthState.Error)?.message
        } else if (authState is AuthState.Authenticated) {
            loginError = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        user?.let {
            Text(
                text = "Logged in as ${user.description}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.logout() }
            ) {
                Text("Logout")
            }
        } ?: run {
            // Show provider selection and auth UI
            if (providers.isNotEmpty()) {
                // Provider tabs
                PrimaryTabRow(
                    selectedTabIndex = providers.indexOf(selectedProvider)
                ) {
                    providers.forEach { provider ->
                        Tab(
                            selected = provider == selectedProvider,
                            onClick = { viewModel.selectProvider(provider) },
                            text = { Text(provider.id.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show provider-specific UI
                selectedProvider?.let { provider ->
                    when (provider.type) {
                        "builtin" -> BuiltinAuthForm(viewModel)
                        else -> Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.login() },
                            enabled = authState !is AuthState.Loading
                        ) {
                            Text("Authorize with ${provider.id.replaceFirstChar { it.uppercase() }}")
                        }
                    }
                }
            } else {
                // No providers loaded yet - show loading or retry
                Text(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    text = "Loading authentication providers...",
                    textAlign = TextAlign.Center
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.loadProviders() }
                ) {
                    Text("Retry Loading Providers")
                }
            }

            // Show loading state
            if (authState is AuthState.Loading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Authenticating...",
                    textAlign = TextAlign.Center
                )
            }
        }

        loginError?.let {
            Logger.e("Error $it")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BuiltinAuthForm(viewModel: AuthenticationViewModel) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column {
        TextField(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            value = username,
            onValueChange = { viewModel.username.value = it },
            label = { Text("Username") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            )
        )

        TextField(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            value = password,
            onValueChange = { viewModel.password.value = it },
            label = { Text("Password") },
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (isPasswordVisible)
                    Icons.Filled.VisibilityOff
                else
                    Icons.Filled.Visibility
                val description = if (isPasswordVisible)
                    "Hide password"
                else
                    "Show password"
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(imageVector = icon, contentDescription = description)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            )
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.login() },
            enabled = username.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Login")
        }
    }
}
