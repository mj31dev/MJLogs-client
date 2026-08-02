package dev.mj31.logger.client.app.usecase.timeline

import dev.mj31.logger.client.domain.model.time.TimeRange

/** How much of the log timeline is actually covered by the screencast. */
data class TimelineOverlap(
    val logRange: TimeRange?,
    val videoRange: TimeRange?,
    val overlap: TimeRange?,
) {

    val hasOverlap: Boolean
        get() = overlap != null
}
