package dev.mj31.logger.client.domain.format.parse

import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.time.Instant
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/** Result of interpreting a single physical line of a log file. */
sealed interface ParsedLine {

    /**
     * A complete record: the line matched the active [LogFormatSpec].
     *
     * [utcOffsetSeconds] is the offset the line wrote beside its time, when it wrote one. The
     * [timestamp] already accounts for it; it is kept so that the time can be shown the way the file
     * shows it.
     */
    data class Record(
        val timestamp: Instant,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val utcOffsetSeconds: Int? = null,
    ) : ParsedLine

    /** A line that does not start a new record, e.g. a stack trace frame; appended to the previous record. */
    data class Continuation(val text: String) : ParsedLine

    /**
     * The row naming the columns of a delimited table.
     *
     * Kept apart from [Continuation] because it is neither a record nor text about one: it belongs to
     * the format, and a reader collecting what a file says before its first record must not mistake
     * it for part of that preamble.
     */
    data class ColumnHeader(val names: List<String>) : ParsedLine
}
