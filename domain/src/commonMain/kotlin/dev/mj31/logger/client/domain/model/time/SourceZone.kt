package dev.mj31.logger.client.domain.model.time

import kotlinx.datetime.TimeZone

/**
 * The time zone a log file's clock runs in, and how that was established.
 *
 * [id] is either an IANA name (`Europe/Berlin`), which follows daylight saving time across the
 * file, or a fixed offset (`UTC+03:00`); both are what [TimeZone.of] accepts, and [idOf] is the only
 * way one is made from text, so an instance never holds a zone that cannot be opened.
 */
data class SourceZone(
    val id: String,
    val origin: ZoneOrigin,
) {

    val timeZone: TimeZone
        get() = TimeZone.of(zoneId = id)

    /** True when times read in this zone are the UTC ones, so a second column would repeat them. */
    val isUtc: Boolean
        get() = id == UTC_ID

    companion object {

        const val UTC_ID: String = "UTC"

        val UTC: SourceZone = SourceZone(id = UTC_ID, origin = ZoneOrigin.DEFAULT)

        /**
         * The canonical id of the zone [text] names, or `null` when it names none.
         *
         * Accepts what people write: `Europe/Berlin`, `UTC`, `Z`, `UTC+3`, `GMT-05:30`, `+0300`. A
         * fixed offset comes back as `UTC±hh:mm` whatever its spelling, so two files in the same
         * offset compare equal, and an offset of zero comes back as `UTC`.
         */
        fun idOf(text: String): String? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.uppercase() in UTC_SPELLINGS) return UTC_ID
            OFFSET.matchEntire(input = trimmed)?.let { match -> return offsetId(match = match) }
            return runCatching { TimeZone.of(zoneId = trimmed).id }.getOrNull()
        }

        /** `UTC±hh:mm` for an offset in seconds, `UTC` for zero. */
        fun offsetId(seconds: Int): String {
            if (seconds == 0) return UTC_ID
            val sign = if (seconds < 0) '-' else '+'
            val minutes = kotlin.math.abs(seconds) / SECONDS_PER_MINUTE
            return "UTC$sign${pad(value = minutes / MINUTES_PER_HOUR)}:${pad(value = minutes % MINUTES_PER_HOUR)}"
        }

        private fun offsetId(match: MatchResult): String? {
            val sign = if (match.groupValues[SIGN] == "-") -1 else 1
            val hours = match.groupValues[HOURS].toInt()
            val minutes = match.groupValues[MINUTES].takeIf { it.isNotEmpty() }?.toInt() ?: 0
            if (hours > MAX_OFFSET_HOURS || minutes >= MINUTES_PER_HOUR) return null
            return offsetId(seconds = sign * (hours * MINUTES_PER_HOUR + minutes) * SECONDS_PER_MINUTE)
        }

        private fun pad(value: Int): String = value.toString().padStart(length = 2, padChar = '0')

        private const val SIGN = 1
        private const val HOURS = 2
        private const val MINUTES = 3
        private const val MAX_OFFSET_HOURS = 18
        private const val MINUTES_PER_HOUR = 60
        private const val SECONDS_PER_MINUTE = 60

        private val UTC_SPELLINGS = setOf("Z", "UTC", "GMT", "UT")

        private val OFFSET = Regex(
            pattern = """(?i)(?:UTC|GMT)?\s*([+-])\s*(\d{1,2})(?::?(\d{2}))?""",
        )
    }
}
