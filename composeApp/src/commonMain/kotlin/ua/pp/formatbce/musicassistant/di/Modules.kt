package ua.pp.formatbce.musicassistant.di

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.data.settings.SettingsRepository
import ua.pp.formatbce.musicassistant.data.settings.provideSettings
import ua.pp.formatbce.musicassistant.data.source.ServiceDatasource
import ua.pp.formatbce.musicassistant.ui.compose.library.LibraryViewModel
import ua.pp.formatbce.musicassistant.ui.compose.main.MainViewModel
import ua.pp.formatbce.musicassistant.ui.compose.settings.SettingsViewModel
import ua.pp.formatbce.musicassistant.ui.theme.ThemeViewModel

val sharedModule = module {
    single { provideSettings() }
    singleOf(::SettingsRepository)
    singleOf(::ServiceClient)
    singleOf(::ServiceDatasource)
    viewModelOf(::ThemeViewModel)
    factory { MainViewModel(get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { LibraryViewModel(get()) }
}