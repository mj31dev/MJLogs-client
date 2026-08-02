package dev.mj31.logger.client.app.usecase.timeline

import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.sync.SyncAnchor
import kotlin.time.Duration.Companion.milliseconds

/** Computes the intersection of the log timeline and the video timeline for the current anchor. */
class ResolveTimelineOverlapUseCase {

    operator fun invoke(logRange: TimeRange?, anchor: SyncAnchor?, videoDurationMillis: Long): TimelineOverlap {
        // A non positive duration means "not known yet", exactly as in MapLogTimeToVideoPositionUseCase:
        // the video timeline is then unbounded and no meaningful intersection can be reported.
        val videoRange = if (anchor == null || videoDurationMillis <= 0) {
            null
        } else {
            TimeRange(
                start = anchor.videoStartInstant,
                end = anchor.videoStartInstant + videoDurationMillis.milliseconds,
            )
        }
        if (logRange == null || videoRange == null) {
            return TimelineOverlap(logRange = logRange, videoRange = videoRange, overlap = null)
        }
        val start = maxOf(a = logRange.start, b = videoRange.start)
        val end = minOf(a = logRange.end, b = videoRange.end)
        return TimelineOverlap(
            logRange = logRange,
            videoRange = videoRange,
            overlap = if (start <= end) TimeRange(start = start, end = end) else null,
        )
    }
}
