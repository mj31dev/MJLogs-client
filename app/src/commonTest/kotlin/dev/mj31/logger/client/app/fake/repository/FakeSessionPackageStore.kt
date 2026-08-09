package dev.mj31.logger.client.app.fake.repository

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.PackageWriteProgress
import dev.mj31.logger.client.domain.session.SessionFile
import dev.mj31.logger.client.domain.session.SessionPackage
import dev.mj31.logger.client.domain.session.SessionPackageStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Session files as a map, so a test can save and reopen one without touching a disk.
 *
 * The write emits a couple of progress values because the point of that flow is the progress; a
 * stub that emitted nothing would let a broken progress bar pass.
 */
class FakeSessionPackageStore : SessionPackageStore {

    private val written = mutableMapOf<String, SessionPackage>()

    var releasedPaths: MutableList<String> = mutableListOf()
        private set

    override fun write(targetPath: String, snapshot: WorkspaceSnapshot): Flow<PackageWriteProgress> = flow {
        emit(PackageWriteProgress(fileName = targetPath, copiedBytes = 0L, totalBytes = TOTAL_BYTES))
        emit(PackageWriteProgress(fileName = targetPath, copiedBytes = TOTAL_BYTES, totalBytes = TOTAL_BYTES))
        written[targetPath] = SessionPackage(
            path = targetPath,
            name = SessionFile.nameOf(path = targetPath),
            snapshot = snapshot,
        )
    }

    override suspend fun read(path: String): SessionPackage =
        requireNotNull(value = written[path]) { "No session file at $path" }

    override suspend fun updateSnapshot(path: String, snapshot: WorkspaceSnapshot) {
        written[path] = requireNotNull(value = written[path]).copy(snapshot = snapshot)
    }

    override suspend fun releaseExtracted(path: String) {
        releasedPaths += path
    }

    private companion object {
        const val TOTAL_BYTES = 100L
    }
}
