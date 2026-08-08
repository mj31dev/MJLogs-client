package dev.mj31.logger.client.domain.source.video

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.media.VideoMetadata

/** Reads what a video file declares about itself, without opening it for playback. */
interface VideoMetadataSource {

    suspend fun read(media: VideoMedia): VideoMetadata?
}
