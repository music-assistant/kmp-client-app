package ua.pp.formatbce.musicassistant.player

interface MediaPlayerListener {
    fun onReady()
    fun onAudioCompleted()
    fun onError()
}