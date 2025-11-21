package io.music_assistant.client.error

import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext

actual fun createTestMediaPlayerController(): MediaPlayerController = MediaPlayerController(PlatformContext())
