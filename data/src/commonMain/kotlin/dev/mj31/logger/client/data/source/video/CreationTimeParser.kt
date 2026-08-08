package dev.mj31.logger.client.data.source.video

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Reads the creation moment a container declares, in the several spellings recorders actually write.
 *
 * `creation_time` is defined as UTC and is written as such by most encoders, in a form ISO-8601
 * parsers accept. Apple additionally writes `com.apple.quicktime.creationdate`, which carries the
 * offset the device was standing in — the one piece of metadata that says outright which time zone
 * the clock on the screen belongs to, which is why it is read first.
 *
 * A recorder that writes a placeholder is treated as one that wrote nothing: the epoch means the
 * clock was never set, not that the recording happened in 1970.
 */
class CreationTimeParser {

    fun parse(metadata: Map<String, String>): ParsedCreationTime? =
        withOffset(raw = metadata[APPLE_KEY]) ?: UTC_KEYS.firstNotNullOfOrNull { key ->
            withoutOffset(raw = metadata[key])
        }

    private fun withOffset(raw: String?): ParsedCreationTime? {
        val text = normalize(raw = raw) ?: return null
        val match = OFFSET_SUFFIX.find(input = text) ?: return null
        val instant = runCatching { Instant.parse(input = text) }.getOrNull()?.takeIf(::isPlausible) ?: return null
        return ParsedCreationTime(instant = instant, offsetMinutes = offsetMinutesOf(match = match))
    }

    private fun withoutOffset(raw: String?): ParsedCreationTime? {
        val text = normalize(raw = raw) ?: return null
        val instant = runCatching { Instant.parse(input = text) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(input = text) }.getOrNull()?.toInstant(timeZone = TimeZone.UTC)
        return instant?.takeIf(::isPlausible)?.let { ParsedCreationTime(instant = it, offsetMinutes = null) }
    }

    /**
     * Brings the two liberties recorders take back to ISO-8601: a space where the standard wants a
     * `T`, and a four digit offset where it wants a colon between the hours and the minutes.
     */
    private fun normalize(raw: String?): String? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return text.replace(oldValue = " ", newValue = "T")
            .replace(regex = COMPACT_OFFSET) { match ->
                "${match.groupValues[SIGN_GROUP]}${match.groupValues[HOUR_GROUP]}:${match.groupValues[MINUTE_GROUP]}"
            }
    }

    /** `Z` is a real offset of zero; anything else is signed hours and minutes. */
    private fun offsetMinutesOf(match: MatchResult): Int {
        val sign = match.groupValues[SIGN_GROUP]
        val hours = match.groupValues[HOUR_GROUP].toIntOrNull()
        val minutes = match.groupValues[MINUTE_GROUP].toIntOrNull()
        if (sign.isEmpty() || hours == null || minutes == null) return 0
        val magnitude = hours * MINUTES_PER_HOUR + minutes
        return if (sign == "-") -magnitude else magnitude
    }

    private fun isPlausible(instant: Instant): Boolean =
        instant.toEpochMilliseconds() > PLACEHOLDER_LIMIT_MILLIS

    private companion object {
        const val APPLE_KEY = "com.apple.quicktime.creationdate"
        const val SIGN_GROUP = 1
        const val HOUR_GROUP = 2
        const val MINUTE_GROUP = 3
        const val MINUTES_PER_HOUR = 60

        /** 1990-01-01: earlier than any screencast and later than every placeholder seen in the wild. */
        const val PLACEHOLDER_LIMIT_MILLIS = 631_152_000_000L

        val UTC_KEYS = listOf("creation_time", "date", APPLE_KEY)

        val COMPACT_OFFSET = Regex(pattern = "([+-])(\\d{2})(\\d{2})$")

        val OFFSET_SUFFIX = Regex(pattern = "(?:Z|([+-])(\\d{2}):(\\d{2}))$")
    }
}
