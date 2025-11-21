package io.music_assistant.client.ui.compose.main

import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext

actual fun createTestMediaPlayerController(): MediaPlayerController = MediaPlayerController(PlatformContext())
