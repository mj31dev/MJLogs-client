package dev.mj31.logger.client.app.usecase.sync.manual

import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.time.Instant

/**
 * Pins an arbitrary wall clock instant to the current video position.
 *
 * The counterpart of [SynchronizeTimelinesUseCase] for frames no log record belongs to: the anchor
 * carries no record id, everything else about the mapping stays the same.
 */
class SynchronizeAtTimestampUseCase(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(timestamp: Instant, videoPositionMillis: Long): SyncAnchor {
        val anchor = SyncAnchor(
            logTimestamp = timestamp,
            videoPositionMillis = videoPositionMillis,
            origin = SyncOrigin.FRAME_TIME,
        )
        syncRepository.setAnchor(anchor = anchor)
        return anchor
    }
}
