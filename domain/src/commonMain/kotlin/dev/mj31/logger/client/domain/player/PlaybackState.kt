package dev.mj31.logger.client.domain.player

/** Immutable snapshot of the playback timeline, independent of the log timeline until synchronized. */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val errorMessage: String? = null,
) {

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    val hasMedia: Boolean
        get() = status != PlaybackStatus.IDLE && status != PlaybackStatus.ERROR

    companion object {
        val IDLE: PlaybackState = PlaybackState()
    }
}
