package dev.mj31.logger.client.data.format.timestamp

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

/**
 * Executable form of a timestamp pattern such as `yyyy-MM-dd HH:mm:ss.SSS`.
 *
 * [regexSource] is a regex fragment without anchors and without an outer capturing group; it exposes
 * one named group per captured [TimestampField] so that it can be embedded into a full line pattern.
 */
class CompiledTimestampPattern internal constructor(
    val pattern: String,
    val regexSource: String,
    val fields: Set<TimestampField>,
) {

    private val hasDateField: Boolean = fields.any { it in TimestampField.dateFields }

    /** Returns the instant described by [match], or `null` when the captured values are not a valid date. */
    fun resolve(match: MatchResult, context: TimestampResolutionContext): Instant? {
        val groups = match.groups
        val epoch = epochOf(groups = groups)
        if (epoch != null) return epoch
        val dateTime = localDateTimeOf(groups = groups, referenceDate = context.referenceDate)
        val offset = offsetOf(groups = groups, defaultMinutes = context.utcOffsetMinutes)
        return if (dateTime == null || offset == null) {
            null
        } else {
            applyMidnightRollover(instant = dateTime.toInstant(offset = offset), previous = context.previous)
        }
    }

    private fun epochOf(groups: MatchGroupCollection): Instant? {
        val millis = valueOf(groups = groups, field = TimestampField.EPOCH_MILLIS)?.toLongOrNull()
        val seconds = valueOf(groups = groups, field = TimestampField.EPOCH_SECONDS)?.toLongOrNull()
        val epochMillis = millis ?: seconds?.times(other = MILLIS_PER_SECOND)
        return epochMillis?.let { Instant.fromEpochMilliseconds(epochMilliseconds = it) }
    }

    private fun localDateTimeOf(groups: MatchGroupCollection, referenceDate: LocalDate): LocalDateTime? {
        val year = valueOf(groups = groups, field = TimestampField.YEAR)?.toIntOrNull()
            ?: valueOf(groups = groups, field = TimestampField.YEAR_SHORT)?.toIntOrNull()?.let { SHORT_YEAR_BASE + it }
            ?: referenceDate.year
        val month = monthOf(groups = groups) ?: referenceDate.monthNumber
        val day = valueOf(groups = groups, field = TimestampField.DAY)?.toIntOrNull() ?: referenceDate.dayOfMonth
        return runCatching {
            LocalDateTime(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
                hour = intOf(groups = groups, field = TimestampField.HOUR),
                minute = intOf(groups = groups, field = TimestampField.MINUTE),
                second = intOf(groups = groups, field = TimestampField.SECOND),
                nanosecond = nanosecondOf(groups = groups),
            )
        }.getOrNull()
    }

    private fun monthOf(groups: MatchGroupCollection): Int? {
        val name = valueOf(groups = groups, field = TimestampField.MONTH_NAME)
        if (name != null) {
            val index = MONTH_NAMES.indexOfFirst { it.equals(other = name, ignoreCase = true) }
            return if (index < 0) null else index + 1
        }
        return valueOf(groups = groups, field = TimestampField.MONTH)?.toIntOrNull()
    }

    private fun nanosecondOf(groups: MatchGroupCollection): Int {
        val fraction = valueOf(groups = groups, field = TimestampField.FRACTION) ?: return 0
        return fraction.padEnd(length = NANOSECOND_DIGITS, padChar = '0').take(n = NANOSECOND_DIGITS).toIntOrNull() ?: 0
    }

    private fun offsetOf(groups: MatchGroupCollection, defaultMinutes: Int): UtcOffset? {
        val raw = valueOf(groups = groups, field = TimestampField.OFFSET)
            ?: return offsetOfMinutes(minutes = defaultMinutes)
        return if (raw.equals(other = "Z", ignoreCase = true)) UtcOffset.ZERO else numericOffsetOf(raw = raw)
    }

    private fun numericOffsetOf(raw: String): UtcOffset? {
        val digits = raw.drop(n = 1).replace(oldValue = ":", newValue = "")
        if (digits.length != OFFSET_DIGITS) return null
        val sign = if (raw.startsWith(prefix = "-")) -1 else 1
        val hours = digits.take(n = OFFSET_HOUR_DIGITS).toIntOrNull()
        val minutes = digits.drop(n = OFFSET_HOUR_DIGITS).toIntOrNull()
        if (hours == null || minutes == null) return null
        return runCatching { UtcOffset(hours = sign * hours, minutes = sign * minutes) }.getOrNull()
    }

    /**
     * Keeps time-only patterns monotonic: when the pattern carries no date at all, a timestamp that
     * lands far before the previous one is moved forward by whole days.
     */
    private fun applyMidnightRollover(instant: Instant, previous: Instant?): Instant {
        if (hasDateField || previous == null) return instant
        val gap = previous.epochSeconds - instant.epochSeconds
        if (gap <= ROLLOVER_THRESHOLD_SECONDS) return instant
        val excess = gap - ROLLOVER_THRESHOLD_SECONDS
        val days = (excess + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY
        return instant + (days * SECONDS_PER_DAY).seconds
    }

    private fun intOf(groups: MatchGroupCollection, field: TimestampField): Int =
        valueOf(groups = groups, field = field)?.toIntOrNull() ?: 0

    private fun valueOf(groups: MatchGroupCollection, field: TimestampField): String? =
        if (field in fields) groups[field.groupName]?.value else null

    private companion object {
        const val SHORT_YEAR_BASE = 2000
        const val MILLIS_PER_SECOND = 1000L
        const val NANOSECOND_DIGITS = 9
        const val OFFSET_DIGITS = 4
        const val OFFSET_HOUR_DIGITS = 2
        const val SECONDS_PER_DAY = 86_400L
        const val ROLLOVER_THRESHOLD_SECONDS = 12 * 60 * 60L
        val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}

private fun offsetOfMinutes(minutes: Int): UtcOffset? =
    runCatching { UtcOffset(hours = minutes / 60, minutes = minutes % 60) }.getOrNull()
