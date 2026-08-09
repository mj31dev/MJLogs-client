package dev.mj31.logger.client.app.usecase.workspace

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import dev.mj31.logger.client.domain.session.SessionPackageStore

/**
 * Decides where a change is written and when.
 *
 * The application store is the live carrier and takes every change immediately, so nothing is ever
 * lost to a crash. The saved file is a different matter: it carries copies of the logs and of the
 * screencast, and no archive format lets a single entry grow in place — rewriting it means copying
 * every byte again, which is not something to do while a slider is being dragged.
 *
 * So the file is written only at the points the user can see: an explicit save, closing the session,
 * leaving the application. In between, the window says the file is behind.
 */
class PersistWorkspaceUseCase(
    private val workspaceRepository: WorkspaceRepository,
    private val packageStore: SessionPackageStore,
) {

    /** Ordinary change: stored, and nowhere else. */
    suspend operator fun invoke(snapshot: WorkspaceSnapshot) {
        workspaceRepository.saveLastWorkspace(snapshot = snapshot)
    }

    /** The playhead alone, written on a timer because it changes many times a second. */
    suspend fun updatePlaybackPosition(positionMillis: Long) {
        workspaceRepository.updatePlaybackPosition(positionMillis = positionMillis)
    }

    /** The moment the file catches up with the screen. */
    suspend fun flushToPackage(snapshot: WorkspaceSnapshot): Boolean {
        val path = snapshot.packagePath ?: return false
        workspaceRepository.saveLastWorkspace(snapshot = snapshot)
        return runCatching { packageStore.updateSnapshot(path = path, snapshot = snapshot) }.isSuccess
    }
}
