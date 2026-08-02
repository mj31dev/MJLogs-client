package dev.mj31.logger.client.app.usecase.timeline

import dev.mj31.logger.client.domain.sync.SyncAnchor
import kotlinx.datetime.Instant

/**
 * Converts a log timestamp into a video position.
 *
 * Returns `null` when the record happened before the recording started or after it ended, which is
 * the normal case when the two timelines only partially overlap.
 */
class MapLogTimeToVideoPositionUseCase {

    operator fun invoke(anchor: SyncAnchor, timestamp: Instant, videoDurationMillis: Long): Long? {
        val position = timestamp.toEpochMilliseconds() - anchor.videoStartInstant.toEpochMilliseconds()
        return if (position < 0 || (videoDurationMillis > 0 && position > videoDurationMillis)) null else position
    }
}
