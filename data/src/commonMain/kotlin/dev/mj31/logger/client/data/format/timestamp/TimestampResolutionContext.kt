package dev.mj31.logger.client.data.format.timestamp

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * Information used to complete timestamps that do not carry every calendar component.
 *
 * @param referenceDate date used when the pattern omits year, month or day.
 * @param zone zone the clock is read in when the pattern carries no explicit offset.
 * @param previous timestamp of the previously parsed line of the same file, used to detect midnight rollover.
 */
data class TimestampResolutionContext(
    val referenceDate: LocalDate,
    val zone: TimeZone = TimeZone.UTC,
    val previous: Instant? = null,
)
