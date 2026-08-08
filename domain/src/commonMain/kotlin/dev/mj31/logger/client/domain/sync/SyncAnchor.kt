package dev.mj31.logger.client.domain.sync

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Correlation point between the two timelines: [logTimestamp] happened exactly at
 * [videoPositionMillis] of the screencast.
 *
 * [origin] records who said so and [accuracyMillis] how far the statement can be off. A human
 * pinning a record to the playhead is exact by definition; a container creation time is worth about
 * a second. Both are anchors all the same, and everything downstream reads them identically.
 */
data class SyncAnchor(
    val logTimestamp: Instant,
    val videoPositionMillis: Long,
    val origin: SyncOrigin,
    val logEntryId: String? = null,
    val accuracyMillis: Long = 0L,
) {

    /** Wall clock instant the video starts at; the whole mapping is derived from it. */
    val videoStartInstant: Instant
        get() = logTimestamp - videoPositionMillis.milliseconds
}
