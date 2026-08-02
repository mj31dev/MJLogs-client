package dev.mj31.logger.client.domain.model.time

import kotlinx.datetime.Instant

/** Inclusive time interval used for session bounds and for time based filtering. */
data class TimeRange(
    val start: Instant,
    val end: Instant,
) {

    val durationMillis: Long
        get() = end.toEpochMilliseconds() - start.toEpochMilliseconds()

    operator fun contains(instant: Instant): Boolean = instant >= start && instant <= end

    companion object {

        fun of(instants: List<Instant>): TimeRange? {
            if (instants.isEmpty()) return null
            return TimeRange(start = instants.min(), end = instants.max())
        }
    }
}
