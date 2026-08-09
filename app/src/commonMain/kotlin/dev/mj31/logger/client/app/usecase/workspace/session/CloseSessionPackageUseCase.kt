package dev.mj31.logger.client.app.usecase.workspace.session

import dev.mj31.logger.client.app.usecase.workspace.PersistWorkspaceUseCase
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.SessionPackageStore

/**
 * Leaves a session file behind cleanly: everything unpacked removed, everything pending written.
 *
 * The unpacked copies go first, and that order is load-bearing rather than arbitrary. Where they
 * live is derived from what the package file currently is, so a flush — which rewrites it — moves
 * the answer: releasing afterwards would delete a folder that was never filled and leave the real
 * one behind for good. Nothing in the flush reads those copies, so there is nothing to lose by
 * dropping them first.
 */
class CloseSessionPackageUseCase(
    private val packageStore: SessionPackageStore,
    private val persistWorkspace: PersistWorkspaceUseCase,
) {

    suspend operator fun invoke(snapshot: WorkspaceSnapshot) {
        val path = snapshot.packagePath ?: return
        runCatching { packageStore.releaseExtracted(path = path) }
        persistWorkspace.flushToPackage(snapshot = snapshot)
    }
}
