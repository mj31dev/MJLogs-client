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

    /**
     * Lets go of whatever is open, leaving the player ready for the next file.
     *
     * Not the same as [release], which ends the player's life: a workspace can be emptied and filled
     * again many times over one run of the application.
     */
    fun close()

    fun play()

    fun pause()

    fun seekTo(positionMillis: Long)

    fun release()
}
