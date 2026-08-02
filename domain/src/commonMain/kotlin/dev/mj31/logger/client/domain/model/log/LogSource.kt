package dev.mj31.logger.client.domain.model.log

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.time.TimeRange

/**
 * One imported log file with the entries produced from it.
 *
 * Sources stay independent of each other so that a single file can be removed or re-parsed with a
 * different [format] without touching the rest of the session.
 */
data class LogSource(
    val id: String,
    val name: String,
    val path: String,
    val format: LogFormatSpec,
    val entries: List<LogEntry>,
    val skippedLineCount: Int = 0,
) {

    val entryCount: Int
        get() = entries.size

    val timeRange: TimeRange?
        get() = TimeRange.of(instants = entries.map { it.timestamp })
}
