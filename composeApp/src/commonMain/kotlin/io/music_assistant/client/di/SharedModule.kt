package io.music_assistant.client.di

import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.settings.provideSettings
import io.music_assistant.client.ui.compose.library.LibraryViewModel
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
    singleOf(::MainDataSource)
    singleOf(::MediaPlayerController)
    viewModelOf(::ThemeViewModel)
    factory { MainViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { LibraryViewModel(get()) }
}