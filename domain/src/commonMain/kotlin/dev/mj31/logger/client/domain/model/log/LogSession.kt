package dev.mj31.logger.client.domain.model.log

import dev.mj31.logger.client.domain.model.time.TimeRange
/**
 * Immutable merged view over every imported [LogSource].
 *
 * [entries] is always ordered by [LogEntry.timestamp]; ties keep the import order of the sources so
 * that interleaved files stay reproducible.
 */
data class LogSession(
    val sources: List<LogSource> = emptyList(),
    val entries: List<LogEntry> = emptyList(),
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val timeRange: TimeRange?
        get() = if (entries.isEmpty()) {
            null
        } else {
            TimeRange(start = entries.first().timestamp, end = entries.last().timestamp)
        }

    fun sourceById(sourceId: String): LogSource? = sources.firstOrNull { it.id == sourceId }

    companion object {
        val EMPTY: LogSession = LogSession()
    }
}
