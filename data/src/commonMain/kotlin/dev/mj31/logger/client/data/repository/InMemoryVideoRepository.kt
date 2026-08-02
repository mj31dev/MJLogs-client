package dev.mj31.logger.client.data.repository

import dev.mj31.logger.client.domain.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.mj31.logger.client.domain.model.media.VideoMedia

/** In-memory holder for the screencast currently loaded into the workspace. */
class InMemoryVideoRepository : VideoRepository {

    private val mediaState = MutableStateFlow<VideoMedia?>(value = null)

    override val media: StateFlow<VideoMedia?> = mediaState.asStateFlow()

    override suspend fun setMedia(media: VideoMedia?) {
        mediaState.value = media
    }
}
