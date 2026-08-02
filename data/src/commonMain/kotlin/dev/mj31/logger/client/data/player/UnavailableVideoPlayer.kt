package dev.mj31.logger.client.data.player

import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.player.VideoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.mj31.logger.client.domain.model.media.VideoMedia

/**
 * Fallback used when no playback backend is available on the machine.
 *
 * The log side of the application stays fully functional; the video pane explains what is missing.
 */
class UnavailableVideoPlayer(
    private val reason: String,
) : VideoPlayer {

    private val playbackState = MutableStateFlow(
        value = PlaybackState(status = PlaybackStatus.ERROR, errorMessage = reason),
    )
    private val frameState = MutableStateFlow<VideoFrame?>(value = null)

    override val state: StateFlow<PlaybackState> = playbackState.asStateFlow()

    override val frames: StateFlow<VideoFrame?> = frameState.asStateFlow()

    override fun open(media: VideoMedia) {
        playbackState.value = PlaybackState(status = PlaybackStatus.ERROR, errorMessage = reason)
    }

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(positionMillis: Long) = Unit

    override fun release() = Unit
}
