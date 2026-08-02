package dev.mj31.logger.client.domain.format.parse

import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlinx.datetime.Instant
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/** Result of interpreting a single physical line of a log file. */
sealed interface ParsedLine {

    /** A complete record: the line matched the active [LogFormatSpec]. */
    data class Record(
        val timestamp: Instant,
        val level: LogLevel,
        val tag: String,
        val message: String,
    ) : ParsedLine

    /** A line that does not start a new record, e.g. a stack trace frame; appended to the previous record. */
    data class Continuation(val text: String) : ParsedLine
}
