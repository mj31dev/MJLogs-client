package dev.mj31.logger.client.domain.player

import dev.mj31.logger.client.domain.model.media.VideoMedia
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform independent video playback port.
 *
 * The UI never talks to a concrete engine; swapping VLC for another backend only requires another
 * implementation of this interface.
 */
interface VideoPlayer {

    val state: StateFlow<PlaybackState>

    val frames: StateFlow<VideoFrame?>

    fun open(media: VideoMedia)

    fun play()

    fun pause()

    fun seekTo(positionMillis: Long)

    fun release()
}
