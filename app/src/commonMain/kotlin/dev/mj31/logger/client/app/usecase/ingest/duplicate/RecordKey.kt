package dev.mj31.logger.client.app.usecase.ingest.duplicate

import dev.mj31.logger.client.domain.model.log.LogEntry
import kotlin.time.Instant

/**
 * What makes two records the same record, whichever file they were read from.
 *
 * The moment and the line as written, with whatever continued it — a stack trace is part of the
 * record it follows, and two exceptions thrown in the same millisecond with different traces are two
 * records. The line number and the source are deliberately left out: they say where a record was
 * found, not what it is.
 */
internal data class RecordKey(val timestamp: Instant, val rawLine: String, val message: String)

internal fun LogEntry.recordKey(): RecordKey = RecordKey(timestamp = timestamp, rawLine = rawLine, message = message)
