package dev.mj31.logger.client.app.usecase.timeline

import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.sync.SyncAnchor

/**
 * Finds the record the video is currently "standing on": the last entry not newer than the mapped
 * instant. Returns `null` while the video is playing before the very first record.
 *
 * [entries] must be sorted by timestamp, which [MergeLogSourcesUseCase] guarantees.
 */
class FindEntryAtVideoPositionUseCase(
    private val mapVideoPositionToLogTime: MapVideoPositionToLogTimeUseCase = MapVideoPositionToLogTimeUseCase(),
) {

    operator fun invoke(entries: List<LogEntry>, anchor: SyncAnchor, videoPositionMillis: Long): LogEntry? {
        if (entries.isEmpty()) return null
        val target = mapVideoPositionToLogTime(anchor = anchor, videoPositionMillis = videoPositionMillis)
        if (target < entries.first().timestamp) return null

        var low = 0
        var high = entries.lastIndex
        var result = 0
        while (low <= high) {
            val middle = (low + high) / 2
            if (entries[middle].timestamp <= target) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return entries[result]
    }
}
