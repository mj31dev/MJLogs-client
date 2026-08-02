package dev.mj31.logger.client.app.fake.repository

import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [SyncRepository] exposing the same [StateFlow] contract as the production one. */
class FakeSyncRepository(
    initialState: SyncState = SyncState.Unsynced,
) : SyncRepository {

    private val mutableSyncState = MutableStateFlow<SyncState>(value = initialState)

    override val syncState: StateFlow<SyncState> = mutableSyncState.asStateFlow()

    var setAnchorCallCount: Int = 0
        private set

    var clearAnchorCallCount: Int = 0
        private set

    override suspend fun setAnchor(anchor: SyncAnchor) {
        setAnchorCallCount++
        mutableSyncState.value = SyncState.Synced(anchor = anchor)
    }

    override suspend fun clearAnchor() {
        clearAnchorCallCount++
        mutableSyncState.value = SyncState.Unsynced
    }
}
