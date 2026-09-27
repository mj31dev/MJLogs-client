package dev.mj31.logger.client.app.view.format

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
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
 * Formats when something was last touched, as `yyyy-MM-dd HH:mm` in the local zone.
 *
 * Deliberately unlike [formatLogDateTime] in both respects. A log timestamp is content: it is shown
 * in UTC and to the millisecond because that is what the file says and because a millisecond is the
 * difference between two records. When a session was last opened is neither — it is a wall clock
 * fact about the person reading it, and no one has ever needed it to the millisecond.
 */
fun formatWallClock(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val time = instant.toLocalDateTime(timeZone = timeZone)
    return "${time.year}-${time.monthNumber.pad(length = PAD_TWO)}-${time.dayOfMonth.pad(length = PAD_TWO)} " +
        "${time.hour.pad(length = PAD_TWO)}:${time.minute.pad(length = PAD_TWO)}"
}

/**
 * The written-out calendar date: `Wednesday, 12 August 2026`.
 *
 * Kept beside the machine forms rather than folded into them because it answers a different
 * question. `2026-08-12` says which date a record carries; this says which day a person lived
 * through, and the weekday is most of that — nobody remembers a recording by its day number.
 */
fun formatCalendarDate(date: LocalDate): String = date.format(format = CalendarDate)

private val CalendarDate: DateTimeFormat<LocalDate> = LocalDate.Format {
    dayOfWeek(names = DayOfWeekNames.ENGLISH_FULL)
    chars(value = ", ")
    day(padding = Padding.NONE)
    char(value = ' ')
    monthName(names = MonthNames.ENGLISH_FULL)
    char(value = ' ')
    year()
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
