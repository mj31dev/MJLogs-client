package dev.mj31.logger.client.app.usecase.workspace

import dev.mj31.logger.client.app.usecase.workspace.session.CloseSessionPackageUseCase
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository
import dev.mj31.logger.client.domain.repository.WorkspaceRepository

/**
 * Empties the workspace so the next file arrives into nothing.
 *
 * The session file the workspace belonged to is left up to date and then let go, rather than being
 * abandoned mid-change: starting something new is not a reason to lose what was open. What is
 * cleared afterwards is only what is on screen and the memory of it.
 */
class ClearWorkspaceUseCase(
    private val closeSessionPackage: CloseSessionPackageUseCase,
    private val sessionRepository: LogSessionRepository,
    private val videoRepository: VideoRepository,
    private val syncRepository: SyncRepository,
    private val workspaceRepository: WorkspaceRepository,
) {

    suspend operator fun invoke(current: WorkspaceSnapshot) {
        closeSessionPackage(snapshot = current)
        sessionRepository.clear()
        videoRepository.setMedia(media = null)
        syncRepository.clearAnchor()
        workspaceRepository.saveLastWorkspace(snapshot = WorkspaceSnapshot.EMPTY)
    }
}
