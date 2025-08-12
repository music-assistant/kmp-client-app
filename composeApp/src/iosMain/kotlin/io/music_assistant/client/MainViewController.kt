package io.music_assistant.client

import androidx.compose.ui.window.ComposeUIViewController
import io.music_assistant.client.di.initKoin
import io.music_assistant.client.di.iosModule
import io.music_assistant.client.ui.compose.App

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin(iosModule())
    }
) { App() }