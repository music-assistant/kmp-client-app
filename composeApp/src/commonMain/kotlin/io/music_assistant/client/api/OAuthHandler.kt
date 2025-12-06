package io.music_assistant.client.api

/**
 * Platform-specific OAuth handler interface.
 * Each platform implements this to open the login URL in a browser.
 */
expect class OAuthHandler() {
    /**
     * Open the OAuth login URL in a browser.
     * On Android, this uses Custom Chrome Tabs or default browser.
     * On iOS, this uses ASWebAuthenticationSession.
     * On Desktop, this opens the system browser.
     */
    fun openLoginUrl(loginUrl: String)

    /**
     * Check if OAuth redirect handling is supported on this platform.
     */
    fun isSupported(): Boolean
}
