package io.music_assistant.client.ui.compose.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.ui.compose.home.HomeScreen
import io.music_assistant.client.ui.compose.settings.SettingsScreen
import io.music_assistant.client.utils.BottomSheetSceneStrategy
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject

sealed interface NavScreen : NavKey {
    @Serializable
    data object Home : NavScreen

    @Serializable
    data object Settings : NavScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val serviceClient: ServiceClient = koinInject()
    val sessionState by serviceClient.sessionState.collectAsStateWithLifecycle()

    // Determine initial screen based on authentication state
    val initialScreen = when (val state = sessionState) {
        is SessionState.Connected -> {
            when (state.dataConnectionState) {
                DataConnectionState.Authenticated -> NavScreen.Home
                else -> NavScreen.Settings
            }
        }

        else -> NavScreen.Settings
    }

    val backStack = rememberNavBackStack(
        SavedStateConfiguration(
            from = SavedStateConfiguration.DEFAULT,
            builderAction = {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(NavScreen.Home::class, NavScreen.Home.serializer())
                        subclass(NavScreen.Settings::class, NavScreen.Settings.serializer())
                    }
                }
            }
        ),
        initialScreen
    )

    // Monitor session state and navigate appropriately
    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Reconnecting -> {
                // Preserve current screen during reconnection - don't navigate
            }

            is SessionState.Disconnected -> {
                // If disconnected and not already on Settings, navigate to Settings
                if (backStack.last() !is NavScreen.Settings) {
                    backStack.clear()
                    backStack.add(NavScreen.Settings)
                }
            }

            is SessionState.Connected -> {
                val connState = (sessionState as SessionState.Connected).dataConnectionState
                when {
                    // If authenticated or anonymous, navigate to Home (clear Settings from backstack)
                    connState == DataConnectionState.Authenticated -> {
                        if (backStack.last() !is NavScreen.Home) {
                            backStack.clear()
                            backStack.add(NavScreen.Home)
                        }
                    }
                    // If not authenticated/anonymous, navigate to Settings
                    backStack.last() !is NavScreen.Settings -> {
                        backStack.clear()
                        backStack.add(NavScreen.Settings)
                    }
                }
            }

            is SessionState.Connecting -> { /* Do nothing */
            }
        }
    }
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategy = bottomSheetStrategy.then(dialogStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(
                rememberSaveableStateHolder()
            )
        ),
        entryProvider = entryProvider {
            entry<NavScreen.Home> {
                HomeScreen(navigateTo = { screen -> backStack.add(screen) })
            }
            entry<NavScreen.Settings> {
                SettingsScreen(
                    onBack = { if (backStack.last() is NavScreen.Settings) backStack.removeLastOrNull() },
                )
            }
        }
    )
}
