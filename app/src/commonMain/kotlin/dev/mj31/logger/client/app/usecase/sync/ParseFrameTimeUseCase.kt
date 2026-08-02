package dev.mj31.logger.client.app.usecase.sync

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Reads the wall clock time the user typed while looking at a frame.
 *
 * A screencast of a device usually shows a clock, a chat bubble or a console, so the user can state
 * what a frame corresponds to even when no log record describes that moment. The accepted spellings
 * are the ones the application itself prints (`yyyy-MM-dd HH:mm:ss.SSS` and `HH:mm:ss.SSS`); a time
 * without a date is completed with [referenceDate], which the loaded session provides.
 *
 * Log timestamps without an explicit offset are read as UTC, so a typed time is read the same way.
 */
class ParseFrameTimeUseCase {

    operator fun invoke(text: String, referenceDate: LocalDate?): Instant? {
        val trimmed = text.trim()
        val dated = DATE_TIME.matchEntire(input = trimmed)
        return if (dated != null) {
            runCatching { LocalDate.parse(input = dated.groupValues[DATE_GROUP]) }
                .getOrNull()
                ?.let { date -> instantOf(date = date, match = dated, timeOffset = DATE_GROUP) }
        } else {
            val timed = TIME_ONLY.matchEntire(input = trimmed)
            if (referenceDate == null || timed == null) {
                null
            } else {
                instantOf(date = referenceDate, match = timed, timeOffset = 0)
            }
        }
    }

    /**
     * Builds the instant from the groups following [timeOffset].
     *
     * The two patterns differ only by the leading date, so their time groups are addressed by
     * position: a named group that one of the patterns does not declare cannot even be asked for.
     */
    private fun instantOf(date: LocalDate, match: MatchResult, timeOffset: Int): Instant? = runCatching {
        LocalDateTime(
            year = date.year,
            monthNumber = date.monthNumber,
            dayOfMonth = date.dayOfMonth,
            hour = match.groupValues[timeOffset + HOUR_GROUP].toInt(),
            minute = match.groupValues[timeOffset + MINUTE_GROUP].toInt(),
            second = match.groupValues[timeOffset + SECOND_GROUP].toIntOrNull() ?: 0,
            nanosecond = millisOf(typed = match.groupValues[timeOffset + MILLIS_GROUP]) * NANOS_PER_MILLI,
        ).toInstant(timeZone = TimeZone.UTC)
    }.getOrNull()

    /** `.5` means half a second, so the typed digits are padded rather than read as they stand. */
    private fun millisOf(typed: String): Int =
        typed.takeIf { it.isNotEmpty() }?.padEnd(length = MILLI_DIGITS, padChar = '0')?.toInt() ?: 0

    private companion object {
        const val MILLI_DIGITS = 3
        const val NANOS_PER_MILLI = 1_000_000

        const val DATE_GROUP = 1
        const val HOUR_GROUP = 1
        const val MINUTE_GROUP = 2
        const val SECOND_GROUP = 3
        const val MILLIS_GROUP = 4

        const val TIME_PATTERN = "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:[.,](\\d{1,3}))?"

        val DATE_TIME = Regex(pattern = "(\\d{4}-\\d{2}-\\d{2})[ T]$TIME_PATTERN")

        val TIME_ONLY = Regex(pattern = TIME_PATTERN)
    }
}
