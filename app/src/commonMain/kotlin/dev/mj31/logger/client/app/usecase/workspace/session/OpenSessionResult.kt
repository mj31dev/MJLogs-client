package dev.mj31.logger.client.app.usecase.workspace.session

import dev.mj31.logger.client.app.usecase.workspace.WorkspaceRestoreResult
import dev.mj31.logger.client.domain.session.SessionPackage

/** Outcome of opening a saved session file. */
sealed interface OpenSessionResult {

    data class Opened(
        val session: SessionPackage,
        val restore: WorkspaceRestoreResult,
    ) : OpenSessionResult

    /**
     * The file could not be opened at all.
     *
     * A package written by a newer build lands here rather than being read partially: a session that
     * silently lost whatever this version does not understand looks exactly like a complete one.
     */
    data class Failed(val message: String) : OpenSessionResult
}
