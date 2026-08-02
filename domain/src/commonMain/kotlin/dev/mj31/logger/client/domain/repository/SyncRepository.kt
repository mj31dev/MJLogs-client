package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.flow.StateFlow

/** Holds the manual synchronization anchor between the log timeline and the video timeline. */
interface SyncRepository {

    val syncState: StateFlow<SyncState>

    suspend fun setAnchor(anchor: SyncAnchor)

    suspend fun clearAnchor()
}
