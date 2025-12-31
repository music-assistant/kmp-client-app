package io.music_assistant.client.di

import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.settings.provideSettings
import io.music_assistant.client.ui.compose.library.LibraryViewModel
import io.music_assistant.client.ui.compose.library2.Library2ViewModel
import io.music_assistant.client.ui.compose.home.HomeScreenViewModel
import io.music_assistant.client.ui.compose.main.MainViewModel
import io.music_assistant.client.ui.compose.settings.SettingsViewModel
import io.music_assistant.client.ui.theme.ThemeViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    single { provideSettings() }
    singleOf(::SettingsRepository)
    singleOf(::ServiceClient)
    singleOf(::MediaPlayerController)  // Used by MainDataSource for Sendspin
    singleOf(::MainDataSource)          // Singleton - held by foreground service
    viewModelOf(::ThemeViewModel)
    factory { MainViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { LibraryViewModel(get(), get()) }
    factory { Library2ViewModel(get()) }
    factory { HomeScreenViewModel(get(), get(), get()) }
}