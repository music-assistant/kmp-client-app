package io.music_assistant.client.utils

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.music_assistant.client.ui.compose.home.HomeScreen
import io.music_assistant.client.ui.compose.library.LibraryArgs
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.compose.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

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
        NavScreen.Main
    )
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
                HomeScreen()
            }
            entry<NavScreen.Settings> {
                SettingsScreen { if (backStack.last() is NavScreen.Settings) backStack.removeLastOrNull() }
            }
            entry<NavScreen.Library>(
                metadata = BottomSheetSceneStrategy.bottomSheet()
            ) {
                LibraryScreen(it.args) { if (backStack.last() is NavScreen.Library) backStack.removeLastOrNull() }
            }
        }
    )
}
