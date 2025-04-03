package io.music_assistant.client.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun provideSettings(): Settings {
    return PreferencesSettings(Preferences.userRoot().node("AppPreferences"))
}