package dev.mj31.logger.client.data.session

import dev.mj31.logger.client.data.workspace.MjLogsDatabaseFactory
import dev.mj31.logger.client.data.workspace.WorkspaceMapping
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Reads and writes the single workspace a session package carries.
 *
 * The package uses the same schema as the application store, so the database it contains is opened
 * by the same Room definition and migrated by the same history. It differs only in holding exactly
 * one workspace and no list of recent files.
 */
internal class PackageSnapshotDatabase(private val dispatcher: CoroutineDispatcher) {

    suspend fun write(databasePath: String, snapshot: WorkspaceSnapshot) {
        withDatabase(databasePath = databasePath) { dao ->
            dao.replaceWorkspace(
                workspace = WorkspaceMapping.toEntity(snapshot = snapshot),
                sources = WorkspaceMapping.toEntities(sources = snapshot.logSources),
            )
        }
    }

    suspend fun read(databasePath: String): WorkspaceSnapshot? = withDatabase(databasePath = databasePath) { dao ->
        val workspace = dao.loadWorkspace() ?: return@withDatabase null
        WorkspaceMapping.toSnapshot(workspace = workspace, sources = dao.loadLogSources())
    }

    /**
     * A package database is opened for one operation and closed again.
     *
     * Keeping it open would hold a file handle on something the user may move or delete at any
     * moment, and the whole point of the file is that it belongs to them rather than to us.
     */
    private suspend fun <T> withDatabase(
        databasePath: String,
        block: suspend (dev.mj31.logger.client.data.workspace.db.WorkspaceDao) -> T,
    ): T {
        val database = MjLogsDatabaseFactory.open(path = databasePath, dispatcher = dispatcher)
        return try {
            block(database.workspaceDao())
        } finally {
            database.close()
        }
    }
}
