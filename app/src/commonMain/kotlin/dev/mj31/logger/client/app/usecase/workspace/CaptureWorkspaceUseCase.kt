package dev.mj31.logger.client.app.usecase.workspace

import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository

/**
 * Reduces everything currently open into the description that can bring it back.
 *
 * The parsed records are dropped on purpose: they are derived from files that are still on disk, and
 * reading them again on restore is both cheaper than storing them and correct when they have grown.
 */
class CaptureWorkspaceUseCase(
    private val sessionRepository: LogSessionRepository,
    private val videoRepository: VideoRepository,
    private val syncRepository: SyncRepository,
) {

    operator fun invoke(
        filter: LogFilter,
        timeWindowMillis: Long?,
        followVideo: Boolean,
        videoPositionMillis: Long,
        packagePath: String?,
    ): WorkspaceSnapshot = WorkspaceSnapshot(
        logSources = sessionRepository.sources.value.map { source ->
            LogSourceRef(id = source.id, name = source.name, path = source.path, format = source.format)
        },
        video = videoRepository.media.value,
        anchor = syncRepository.syncState.value.anchorOrNull,
        filter = filter,
        timeWindowMillis = timeWindowMillis,
        followVideo = followVideo,
        videoPositionMillis = videoPositionMillis,
        packagePath = packagePath,
    )
}
