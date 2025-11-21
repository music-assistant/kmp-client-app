package io.music_assistant.client.data

import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext

actual fun createTestMediaPlayerController(): MediaPlayerController = MediaPlayerController(PlatformContext())
