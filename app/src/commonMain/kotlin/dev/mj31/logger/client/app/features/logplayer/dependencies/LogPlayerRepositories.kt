package dev.mj31.logger.client.app.features.logplayer.dependencies

import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository

/** Repositories the screen depends on, bundled to keep the constructor readable. */
data class LogPlayerRepositories(
    val session: LogSessionRepository,
    val video: VideoRepository,
    val sync: SyncRepository,
)
