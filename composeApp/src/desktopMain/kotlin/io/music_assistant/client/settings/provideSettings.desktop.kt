package io.music_assistant.client.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun provideSettings(): Settings = PreferencesSettings(Preferences.userRoot().node("AppPreferences"))
