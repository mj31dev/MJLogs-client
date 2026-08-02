package dev.mj31.logger.client.app.features.logplayer.state.ui

import dev.mj31.logger.client.domain.player.PlaybackStatus

/** Presentation model of the screencast pane. */
data class VideoUiState(
    val name: String? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val errorMessage: String? = null,
) {

    val hasVideo: Boolean
        get() = name != null && status != PlaybackStatus.ERROR

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING
}
