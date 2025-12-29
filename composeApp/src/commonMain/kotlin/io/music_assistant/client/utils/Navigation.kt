package io.music_assistant.client.utils

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
import io.music_assistant.client.ui.compose.library.LibraryArgs
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.compose.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject

sealed interface NavScreen : NavKey {
    @Serializable
    data object Main : NavScreen

    @Serializable
    data object Home : NavScreen

    @Serializable
    data object Settings : NavScreen

    @Serializable
    data class Library(val args: LibraryArgs) : NavScreen
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
                        subclass(NavScreen.Main::class, NavScreen.Main.serializer())
                        subclass(NavScreen.Home::class, NavScreen.Home.serializer())
                        subclass(NavScreen.Settings::class, NavScreen.Settings.serializer())
                        subclass(NavScreen.Library::class, NavScreen.Library.serializer())
                    }
                }
            }
        ),
        initialScreen
    )

    // Monitor session state and navigate appropriately
    LaunchedEffect(sessionState) {
        when (sessionState) {
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
            is SessionState.Connecting -> { /* Do nothing */ }
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
            entry<NavScreen.Main> {
                MainScreen { screen -> backStack.add(screen) }
            }
            entry<NavScreen.Home> {
                HomeScreen(navigateTo = { screen -> backStack.add(screen) })
            }
            entry<NavScreen.Settings> {
                SettingsScreen(
                    onBack = { if (backStack.last() is NavScreen.Settings) backStack.removeLastOrNull() },
                )
            }
            entry<NavScreen.Library>(
                metadata = BottomSheetSceneStrategy.bottomSheet()
            ) {
                LibraryScreen(it.args) { if (backStack.last() is NavScreen.Library) backStack.removeLastOrNull() }
            }
        }
    )
}
