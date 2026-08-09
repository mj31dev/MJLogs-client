package dev.mj31.logger.client.app.fake.repository

import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory stand-in for the durable store.
 *
 * [stored] is exposed so a test can both seed a workspace to restore and assert on what was written.
 */
class FakeWorkspaceRepository(
    var stored: WorkspaceSnapshot? = null,
) : WorkspaceRepository {

    private val packages = MutableStateFlow<List<RecentPackage>>(value = emptyList())

    var saveCount: Int = 0
        private set

    var positionWriteCount: Int = 0
        private set

    override suspend fun loadLastWorkspace(): WorkspaceSnapshot? = stored

    override suspend fun saveLastWorkspace(snapshot: WorkspaceSnapshot) {
        stored = snapshot
        saveCount++
    }

    override suspend fun updatePlaybackPosition(positionMillis: Long) {
        stored = stored?.copy(videoPositionMillis = positionMillis)
        positionWriteCount++
    }

    override val recentPackages: Flow<List<RecentPackage>> = packages.asStateFlow()

    override suspend fun rememberPackage(entry: RecentPackage) {
        packages.update { current -> listOf(entry) + current.filterNot { it.path == entry.path } }
    }

    override suspend fun forgetPackage(path: String) {
        packages.update { current -> current.filterNot { it.path == path } }
    }
}
