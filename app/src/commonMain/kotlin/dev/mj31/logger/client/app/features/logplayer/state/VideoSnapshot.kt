package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.player.PlaybackState

/** Current state of the video pane sources. */
data class VideoSnapshot(
    val media: VideoMedia?,
    val playback: PlaybackState,
)
