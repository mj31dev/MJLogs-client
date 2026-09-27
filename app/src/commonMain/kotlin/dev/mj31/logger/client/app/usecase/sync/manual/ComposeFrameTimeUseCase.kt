package dev.mj31.logger.client.app.usecase.sync.manual

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Turns a date and a time picked with the mouse into the text of the frame time field.
 *
 * A picker only offers hours and minutes, while a log record is located to the millisecond, so the
 * seconds already typed in the field survive the picking: the calendar sets the coarse moment, the
 * keyboard refines it.
 */
class ComposeFrameTimeUseCase(
    private val parseFrameTime: ParseFrameTimeUseCase,
) {

    /**
     * [dateMillis] is midnight UTC of the picked day, which is how a date picker reports a day;
     * [timeZone] is the one the field's text is read in.
     */
    operator fun invoke(
        dateMillis: Long,
        hour: Int,
        minute: Int,
        previousText: String,
        timeZone: TimeZone = TimeZone.UTC,
    ): String {
        val date = Instant.fromEpochMilliseconds(epochMilliseconds = dateMillis)
            .toLocalDateTime(timeZone = TimeZone.UTC)
            .date
        val previous = parseFrameTime(text = previousText, referenceDate = date, timeZone = timeZone)
            ?.toLocalDateTime(timeZone = timeZone)
        val second = previous?.second ?: 0
        val millis = previous?.nanosecond?.div(other = NANOS_PER_MILLI) ?: 0

        return buildString {
            append(date.toString())
            append(' ')
            append(hour.pad(length = PAD_TWO))
            append(':')
            append(minute.pad(length = PAD_TWO))
            append(':')
            append(second.pad(length = PAD_TWO))
            append('.')
            append(millis.pad(length = PAD_THREE))
        }
    }

    private fun Int.pad(length: Int): String = toString().padStart(length = length, padChar = '0')

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000
        const val PAD_TWO = 2
        const val PAD_THREE = 3
    }
}
