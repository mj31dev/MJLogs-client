package dev.mj31.logger.client.data.sync.screen

import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime
import kotlinx.datetime.LocalTime

/**
 * Turns whatever a recognizer produced for a status bar into a time, or into nothing.
 *
 * Two spellings reach this class, `21:41` and `9:41 PM`, because a device shows whichever its locale
 * asks for. Everything around the digits is noise: a battery percentage, a carrier name, a stray
 * glyph from the edge of the crop. The parser therefore searches rather than validates, and refuses
 * a string that holds no plausible time at all.
 *
 * The letter-shaped confusions a recognizer makes on digits are undone first: inside a crop that is
 * known to contain a clock, an `O` is a zero and an `l` is a one far more often than they are
 * letters.
 *
 * What it will not do is decide which half of the day a bare `2:39` belongs to; it only reports
 * whether the question arises. See [ScreenClockTime] for why the leading zero answers it and the
 * missing `PM` does not.
 */
class ClockTextParser {

    fun parse(text: String): ScreenClockTime? {
        val meridiem = meridiemOf(text = text)
        val match = TIME.find(input = normalizeDigits(text = text)) ?: return null
        val digits = match.groupValues[HOUR_GROUP]
        val minute = match.groupValues[MINUTE_GROUP].toIntOrNull()?.takeIf { it in MINUTE_RANGE }
        val hour = digits.toIntOrNull()?.let { read -> resolveHour(hour = read, meridiem = meridiem) }

        return if (hour == null || minute == null) {
            null
        } else {
            ScreenClockTime(
                time = LocalTime(hour = hour, minute = minute),
                isHalfDayAmbiguous = isAmbiguous(digits = digits, hour = hour, meridiem = meridiem),
            )
        }
    }

    /**
     * Whether the dial could equally have meant the other half of the day.
     *
     * A padded hour comes from a twenty-four hour dial, which leaves nothing to decide. A bare digit
     * comes from a twelve hour one, which decides nothing. `10`, `11` and `12` are the overlap: both
     * dials write them the same way, so they stay open unless a meridiem closed them.
     */
    private fun isAmbiguous(digits: String, hour: Int, meridiem: Meridiem?): Boolean = when {
        meridiem != null -> false
        digits.length == 1 -> true
        else -> hour in BOTH_DIALS
    }

    /**
     * `AM` and `PM` are looked for before the digit repair, which would otherwise have to be taught
     * to leave them alone; no substitution in [normalizeDigits] touches `A`, `P` or `M`.
     */
    private fun meridiemOf(text: String): Meridiem? = when {
        MERIDIEM_AM.containsMatchIn(input = text) -> Meridiem.AM
        MERIDIEM_PM.containsMatchIn(input = text) -> Meridiem.PM
        else -> null
    }

    private fun resolveHour(hour: Int, meridiem: Meridiem?): Int? = when (meridiem) {
        null -> hour.takeIf { it in HOUR_RANGE_24 }
        Meridiem.AM -> hour.takeIf { it in HOUR_RANGE_12 }?.let { if (it == NOON) 0 else it }
        Meridiem.PM -> hour.takeIf { it in HOUR_RANGE_12 }?.let { if (it == NOON) NOON else it + NOON }
    }

    private fun normalizeDigits(text: String): String = text.map { character ->
        SUBSTITUTIONS[character] ?: character
    }.joinToString(separator = "")

    private enum class Meridiem { AM, PM }

    private companion object {
        const val HOUR_GROUP = 1
        const val MINUTE_GROUP = 2
        const val NOON = 12

        val HOUR_RANGE_24 = 0..23
        val HOUR_RANGE_12 = 1..12
        val MINUTE_RANGE = 0..59

        /** Hours a twelve hour dial and a twenty-four hour dial spell identically. */
        val BOTH_DIALS = 10..12

        /**
         * The separator is deliberately loose: a colon rendered at status bar size is one of the
         * first things a recognizer turns into a full stop or drops to a bare space.
         */
        val TIME = Regex(pattern = "(\\d{1,2})\\s*[:.,;]?\\s*(\\d{2})")

        val MERIDIEM_AM = Regex(pattern = "\\bA\\.?\\s?M\\.?", option = RegexOption.IGNORE_CASE)
        val MERIDIEM_PM = Regex(pattern = "\\bP\\.?\\s?M\\.?", option = RegexOption.IGNORE_CASE)

        val SUBSTITUTIONS: Map<Char, Char> = mapOf(
            'O' to '0',
            'o' to '0',
            'D' to '0',
            'Q' to '0',
            'l' to '1',
            'I' to '1',
            '|' to '1',
            'i' to '1',
            'Z' to '2',
            'S' to '5',
            's' to '5',
            'G' to '6',
            'B' to '8',
        )
    }
}
