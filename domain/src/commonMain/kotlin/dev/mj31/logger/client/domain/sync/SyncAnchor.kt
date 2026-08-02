package dev.mj31.logger.client.domain.sync

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant

/**
 * Manual correlation point between the two timelines: the user states that [logTimestamp] happened
 * exactly at [videoPositionMillis] of the screencast.
 */
data class SyncAnchor(
    val logTimestamp: Instant,
    val videoPositionMillis: Long,
    val logEntryId: String? = null,
) {

    /** Wall clock instant the video starts at; the whole mapping is derived from it. */
    val videoStartInstant: Instant
        get() = logTimestamp - videoPositionMillis.milliseconds
}
