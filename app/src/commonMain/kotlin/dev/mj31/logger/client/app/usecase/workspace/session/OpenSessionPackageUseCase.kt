package dev.mj31.logger.client.app.usecase.workspace.session

import dev.mj31.logger.client.app.usecase.workspace.RestoreWorkspaceUseCase
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import dev.mj31.logger.client.domain.session.SessionPackageStore
import kotlin.time.Clock

/** Opens a saved session file and makes its workspace the one on screen. */
class OpenSessionPackageUseCase(
    private val packageStore: SessionPackageStore,
    private val restoreWorkspace: RestoreWorkspaceUseCase,
    private val workspaceRepository: WorkspaceRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(path: String): OpenSessionResult {
        val opened = runCatching { packageStore.read(path = path) }
            .getOrElse { failure -> return OpenSessionResult.Failed(message = messageOf(failure = failure)) }
        val restore = restoreWorkspace(snapshot = opened.snapshot)
        workspaceRepository.rememberPackage(
            entry = RecentPackage(
                path = opened.path,
                name = opened.name,
                lastOpened = clock.now(),
            ),
        )
        return OpenSessionResult.Opened(session = opened, restore = restore)
    }

    /** The reader states precisely what is wrong; a generic wording would throw that away. */
    private fun messageOf(failure: Throwable): String =
        failure.message ?: "The session file could not be opened."
}
