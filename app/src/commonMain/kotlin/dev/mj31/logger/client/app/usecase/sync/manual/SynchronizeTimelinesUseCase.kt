package dev.mj31.logger.client.app.usecase.sync.manual

import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin

/**
 * Pins the selected log record to the current video position.
 *
 * Until this use case runs, both timelines move independently, which is the behaviour required for
 * the PoC's manual synchronization mode.
 */
class SynchronizeTimelinesUseCase(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(entry: LogEntry, videoPositionMillis: Long): SyncAnchor {
        val anchor = SyncAnchor(
            logTimestamp = entry.timestamp,
            videoPositionMillis = videoPositionMillis,
            origin = SyncOrigin.SELECTED_ENTRY,
            logEntryId = entry.id,
        )
        syncRepository.setAnchor(anchor = anchor)
        return anchor
    }
}
