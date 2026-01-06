package io.music_assistant.client.auth

actual class OAuthHandler {
    actual fun openOAuthUrl(url: String) {
        // Not yet supported on iOS
        throw UnsupportedOperationException("OAuth not yet supported on iOS")
    }
}
