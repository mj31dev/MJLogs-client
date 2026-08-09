package dev.mj31.logger.client.app.usecase.workspace.session

import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import dev.mj31.logger.client.domain.session.PackageWriteProgress
import dev.mj31.logger.client.domain.session.SessionFile
import dev.mj31.logger.client.domain.session.SessionPackageStore
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Writes the workspace into a file the user can keep and hand around.
 *
 * The file is only remembered once the write has finished. A save that was cancelled halfway leaves
 * no file behind, and listing a file that does not exist would be worse than not listing it.
 */
class SaveSessionPackageUseCase(
    private val packageStore: SessionPackageStore,
    private val workspaceRepository: WorkspaceRepository,
    private val clock: Clock,
) {

    /** The chooser returns whatever the user typed, which need not carry the extension. */
    fun targetPathFor(path: String): String = SessionFile.withExtension(path = path)

    operator fun invoke(targetPath: String, snapshot: WorkspaceSnapshot): Flow<PackageWriteProgress> = flow {
        emitAll(
            flow = packageStore.write(
                targetPath = targetPath,
                snapshot = snapshot.copy(packagePath = targetPath),
            ),
        )
        workspaceRepository.rememberPackage(
            entry = RecentPackage(
                path = targetPath,
                name = SessionFile.nameOf(path = targetPath),
                lastOpened = clock.now(),
            ),
        )
    }
}
