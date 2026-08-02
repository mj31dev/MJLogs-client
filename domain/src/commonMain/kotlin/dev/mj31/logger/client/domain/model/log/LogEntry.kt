package dev.mj31.logger.client.domain.model.log

import kotlinx.datetime.Instant

/**
 * A single normalized log record belonging to a [LogSource].
 *
 * Everything that is not a timestamp, a level or a tag is kept in [message] as required by the
 * PoC specification ("time, level and everything else is the body").
 */
data class LogEntry(
    val id: String,
    val sourceId: String,
    val lineNumber: Int,
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val rawLine: String,
) {

    /** Text used by free-text filtering; kept as a property so the filter stays allocation free. */
    fun matchesText(query: String): Boolean =
        tag.contains(other = query, ignoreCase = true) ||
            message.contains(other = query, ignoreCase = true) ||
            rawLine.contains(other = query, ignoreCase = true)
}
