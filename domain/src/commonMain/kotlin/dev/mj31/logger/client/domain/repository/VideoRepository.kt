package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.model.media.VideoMedia
import kotlinx.coroutines.flow.StateFlow

/** Holds the screencast currently loaded into the workspace. */
interface VideoRepository {

    val media: StateFlow<VideoMedia?>

    suspend fun setMedia(media: VideoMedia?)
}
