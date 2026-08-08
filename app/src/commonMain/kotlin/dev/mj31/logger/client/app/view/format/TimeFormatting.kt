package dev.mj31.logger.client.app.view.format

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val PAD_TWO = 2
private const val PAD_THREE = 3
private const val NANOS_PER_MILLI = 1_000_000

/** Formats a video position as `mm:ss.S` or `h:mm:ss.S`. */
fun formatVideoPosition(positionMillis: Long): String {
    val safe = positionMillis.coerceAtLeast(minimumValue = 0L)
    val totalSeconds = safe / MILLIS_PER_SECOND
    val tenths = safe % MILLIS_PER_SECOND / 100
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
    val hours = totalSeconds / SECONDS_PER_MINUTE / MINUTES_PER_HOUR
    val head = if (hours > 0) "$hours:${minutes.pad(length = PAD_TWO)}" else "$minutes"
    return "$head:${seconds.pad(length = PAD_TWO)}.$tenths"
}

/**
 * Formats a log timestamp as `HH:mm:ss.SSS` in [timeZone].
 *
 * Timestamps without an explicit offset are parsed as UTC, so rendering them in UTC by default
 * shows exactly what the log file contains.
 */
fun formatLogTime(instant: Instant, timeZone: TimeZone = TimeZone.UTC): String {
    val time = instant.toLocalDateTime(timeZone = timeZone)
    return buildString {
        append(time.hour.pad(length = PAD_TWO))
        append(':')
        append(time.minute.pad(length = PAD_TWO))
        append(':')
        append(time.second.pad(length = PAD_TWO))
        append('.')
        append((time.nanosecond / NANOS_PER_MILLI).pad(length = PAD_THREE))
    }
}

/** Formats a log timestamp as `yyyy-MM-dd HH:mm:ss.SSS` in [timeZone]. */
fun formatLogDateTime(instant: Instant, timeZone: TimeZone = TimeZone.UTC): String {
    val time = instant.toLocalDateTime(timeZone = timeZone)
    return "${time.year}-${time.monthNumber.pad(length = PAD_TWO)}-${time.dayOfMonth.pad(length = PAD_TWO)} " +
        formatLogTime(instant = instant, timeZone = timeZone)
}

/**
 * Formats how far an anchor may be off, in the unit that makes it legible.
 *
 * Sub-second uncertainty is what separates an anchor that lands on the right frame from one that
 * lands on the right second, and printing it as `0.2s` rather than `200ms` would hide the very
 * distinction the automatic synchronization exists to make.
 */
fun formatAccuracy(millis: Long): String {
    val safe = millis.coerceAtLeast(minimumValue = 0L)
    return if (safe < MILLIS_PER_SECOND) "${safe}ms" else "${safe / MILLIS_PER_SECOND}s"
}

private fun Int.pad(length: Int): String = toString().padStart(length = length, padChar = '0')

private fun Long.pad(length: Int): String = toString().padStart(length = length, padChar = '0')
