package dev.mj31.logger.client.data.format.timestamp

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Information used to complete timestamps that do not carry every calendar component.
 *
 * @param referenceDate date used when the pattern omits year, month or day.
 * @param utcOffsetMinutes offset applied when the pattern carries no explicit offset.
 * @param previous timestamp of the previously parsed line of the same file, used to detect midnight rollover.
 */
data class TimestampResolutionContext(
    val referenceDate: LocalDate,
    val utcOffsetMinutes: Int = 0,
    val previous: Instant? = null,
)
