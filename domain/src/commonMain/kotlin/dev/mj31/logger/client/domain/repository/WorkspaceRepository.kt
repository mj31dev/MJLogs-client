package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Durable memory of the application itself: the workspace last open, and the session files visited.
 *
 * It deliberately knows nothing about *when* to write. Everything except the playhead is worth
 * storing the moment it changes, while the playhead moves many times a second and is written on a
 * timer instead — but that is a policy of the application layer, so the port only offers the two
 * operations and lets the caller decide the rhythm.
 */
interface WorkspaceRepository {

    /** The workspace open when the application was last closed, or `null` on a first run. */
    suspend fun loadLastWorkspace(): WorkspaceSnapshot?

    suspend fun saveLastWorkspace(snapshot: WorkspaceSnapshot)

    /** Cheap partial write for the one field that changes continuously. */
    suspend fun updatePlaybackPosition(positionMillis: Long)

    /** Saved session files that have been opened, most recent first. */
    val recentPackages: Flow<List<RecentPackage>>

    suspend fun rememberPackage(entry: RecentPackage)

    suspend fun forgetPackage(path: String)
}
