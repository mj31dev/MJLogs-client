package dev.mj31.logger.client.app.fake

import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.player.VideoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import dev.mj31.logger.client.domain.model.media.VideoMedia

/**
 * Scriptable [VideoPlayer] used to drive the video timeline from a test.
 *
 * The playback timeline never advances on its own: a test moves the playhead explicitly through
 * [setPosition], which is exactly what makes the "two independent timelines" behaviour observable.
 */
class FakeVideoPlayer : VideoPlayer {

    /** Mutable backing state, exposed so a test can simulate any playback situation. */
    val playbackState: MutableStateFlow<PlaybackState> = MutableStateFlow(value = PlaybackState.IDLE)

    /** Mutable backing frame stream, exposed for the rare test that asserts on decoded frames. */
    val videoFrames: MutableStateFlow<VideoFrame?> = MutableStateFlow(value = null)

    override val state: StateFlow<PlaybackState> = playbackState

    override val frames: StateFlow<VideoFrame?> = videoFrames

    private val mutableOpenedMedia = mutableListOf<VideoMedia>()
    private val mutableSeekPositions = mutableListOf<Long>()

    val openedMedia: List<VideoMedia>
        get() = mutableOpenedMedia.toList()

    /** Every position [seekTo] was called with, in call order. */
    val seekPositions: List<Long>
        get() = mutableSeekPositions.toList()

    var playCallCount: Int = 0
        private set

    var pauseCallCount: Int = 0
        private set

    var releaseCallCount: Int = 0
        private set

    var closeCallCount: Int = 0
        private set

    override fun open(media: VideoMedia) {
        mutableOpenedMedia += media
        playbackState.update { it.copy(status = PlaybackStatus.READY, errorMessage = null) }
    }

    /** Recorded rather than ignored: emptying the workspace has to let the screencast go. */
    override fun close() {
        closeCallCount++
        mutableOpenedMedia.clear()
        playbackState.value = PlaybackState.IDLE
    }

    override fun play() {
        playCallCount++
        playbackState.update { it.copy(status = PlaybackStatus.PLAYING) }
    }

    override fun pause() {
        pauseCallCount++
        playbackState.update { it.copy(status = PlaybackStatus.PAUSED) }
    }

    override fun seekTo(positionMillis: Long) {
        mutableSeekPositions += positionMillis
    }

    override fun release() {
        releaseCallCount++
        playbackState.value = PlaybackState.IDLE
    }

    /** Moves the playhead without touching any other part of the playback state. */
    fun setPosition(positionMillis: Long) {
        playbackState.update { it.copy(positionMillis = positionMillis) }
    }

    fun setDuration(durationMillis: Long) {
        playbackState.update { it.copy(durationMillis = durationMillis) }
    }

    fun setStatus(status: PlaybackStatus, errorMessage: String? = null) {
        playbackState.update { it.copy(status = status, errorMessage = errorMessage) }
    }
}
