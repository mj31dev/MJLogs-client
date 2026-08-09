package dev.mj31.logger.client.data.session

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.PackageWriteProgress
import dev.mj31.logger.client.domain.session.SessionPackage
import dev.mj31.logger.client.domain.session.SessionPackageStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Session files, stored as archives.
 *
 * The archive is a deliberate choice over a database holding the media: a screencast runs to
 * hundreds of megabytes, which no single database value should ever be, and an archive can be
 * opened, listed and repaired with tools every platform already has.
 */
class ZipSessionPackageStore(
    cacheDirectory: File,
    private val dispatcher: CoroutineDispatcher,
) : SessionPackageStore {

    private val snapshotDatabase = PackageSnapshotDatabase(dispatcher = dispatcher)
    private val reader = SessionPackageReader(cacheDirectory = cacheDirectory, snapshotDatabase = snapshotDatabase)
    private val writer = SessionPackageWriter(cacheDirectory = cacheDirectory, snapshotDatabase = snapshotDatabase)

    override fun write(targetPath: String, snapshot: WorkspaceSnapshot): Flow<PackageWriteProgress> = writer
        .write(targetPath = targetPath, snapshot = snapshot)
        .flowOn(context = dispatcher)

    override suspend fun read(path: String): SessionPackage =
        withContext(context = dispatcher) { reader.read(path = path) }

    override suspend fun updateSnapshot(path: String, snapshot: WorkspaceSnapshot) {
        withContext(context = dispatcher) { writer.updateSnapshot(path = path, snapshot = snapshot) }
    }

    override suspend fun releaseExtracted(path: String) {
        withContext(context = dispatcher) { reader.releaseExtracted(path = path) }
    }
}
