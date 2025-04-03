package io.music_assistant.client.di

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.settings.provideSettings
import io.music_assistant.client.data.ServiceDataSource
import io.music_assistant.client.ui.compose.library.LibraryViewModel
import io.music_assistant.client.ui.compose.main.MainViewModel
import io.music_assistant.client.ui.compose.settings.SettingsViewModel
import io.music_assistant.client.ui.theme.ThemeViewModel

val sharedModule = module {
    single { provideSettings() }
    singleOf(::SettingsRepository)
    singleOf(::ServiceClient)
    singleOf(::ServiceDataSource)
    viewModelOf(::ThemeViewModel)
    factory { MainViewModel(get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { LibraryViewModel(get()) }
}