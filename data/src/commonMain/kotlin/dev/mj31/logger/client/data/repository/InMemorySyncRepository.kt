package dev.mj31.logger.client.data.repository

import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory holder of the manual synchronization anchor. */
class InMemorySyncRepository : SyncRepository {

    private val state = MutableStateFlow<SyncState>(value = SyncState.Unsynced)

    override val syncState: StateFlow<SyncState> = state.asStateFlow()

    override suspend fun setAnchor(anchor: SyncAnchor) {
        state.value = SyncState.Synced(anchor = anchor)
    }

    override suspend fun clearAnchor() {
        state.value = SyncState.Unsynced
    }
}
