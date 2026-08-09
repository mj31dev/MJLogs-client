package dev.mj31.logger.client.data.workspace

import dev.mj31.logger.client.data.workspace.db.MjLogsDatabase
import dev.mj31.logger.client.data.workspace.db.entity.RecentPackageEntity
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The application's own store: what was open last, and which session files have been visited. */
class RoomWorkspaceRepository(
    private val database: MjLogsDatabase,
    private val dispatcher: CoroutineDispatcher,
) : WorkspaceRepository {

    private val dao = database.workspaceDao()

    override suspend fun loadLastWorkspace(): WorkspaceSnapshot? = withContext(context = dispatcher) {
        val workspace = dao.loadWorkspace() ?: return@withContext null
        WorkspaceMapping.toSnapshot(workspace = workspace, sources = dao.loadLogSources())
    }

    override suspend fun saveLastWorkspace(snapshot: WorkspaceSnapshot) {
        withContext(context = dispatcher) {
            dao.replaceWorkspace(
                workspace = WorkspaceMapping.toEntity(snapshot = snapshot),
                sources = WorkspaceMapping.toEntities(sources = snapshot.logSources),
            )
        }
    }

    override suspend fun updatePlaybackPosition(positionMillis: Long) {
        withContext(context = dispatcher) {
            dao.updatePlaybackPosition(positionMillis = positionMillis)
        }
    }

    override val recentPackages: Flow<List<RecentPackage>> = dao.observeRecentPackages()
        .map { entities -> entities.mapNotNull(::toRecent) }

    override suspend fun rememberPackage(entry: RecentPackage) {
        withContext(context = dispatcher) {
            dao.upsertRecentPackage(
                entry = RecentPackageEntity(
                    path = entry.path,
                    name = entry.name,
                    lastOpenedMillis = entry.lastOpened.toEpochMilliseconds(),
                ),
            )
        }
    }

    override suspend fun forgetPackage(path: String) {
        withContext(context = dispatcher) { dao.deleteRecentPackage(path = path) }
    }

    private fun toRecent(entity: RecentPackageEntity): RecentPackage = RecentPackage(
        path = entity.path,
        name = entity.name,
        lastOpened = Instant.fromEpochMilliseconds(epochMilliseconds = entity.lastOpenedMillis),
    )
}
