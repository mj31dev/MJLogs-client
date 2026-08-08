package dev.mj31.logger.client.domain.source.video

import dev.mj31.logger.client.domain.model.media.VideoMedia

/**
 * Opens a video file for random access to its frames.
 *
 * Deliberately separate from [dev.mj31.logger.client.domain.player.VideoPlayer]: the player owns one
 * decoder confined to one thread and walks it forwards, so jumping around inside it to inspect a
 * dozen frames would fight with whatever the user is watching. A scan is its own decoder over the
 * same file, and the two never meet.
 */
interface VideoFrameScanner {

    suspend fun open(media: VideoMedia): VideoScan?
}
