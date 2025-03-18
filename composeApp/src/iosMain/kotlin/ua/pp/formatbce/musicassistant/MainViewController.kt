package ua.pp.formatbce.musicassistant

import androidx.compose.ui.window.ComposeUIViewController
import ua.pp.formatbce.musicassistant.di.initKoin
import ua.pp.formatbce.musicassistant.ui.compose.App

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }