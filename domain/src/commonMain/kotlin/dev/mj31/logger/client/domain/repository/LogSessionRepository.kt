package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.model.log.LogSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the imported log files. The PoC keeps everything in memory; a database backed implementation
 * only has to satisfy the same contract.
 *
 * Storage keeps the sources apart; merging them into one chronological session is a decision of the
 * application layer, not of the store behind this port.
 */
interface LogSessionRepository {

    val sources: StateFlow<List<LogSource>>

    suspend fun addSource(source: LogSource)

    suspend fun replaceSource(source: LogSource)

    suspend fun removeSource(sourceId: String)

    suspend fun clear()
}
