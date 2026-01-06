package io.music_assistant.client.auth

expect class OAuthHandler {
    fun openOAuthUrl(url: String)
}
