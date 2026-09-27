package dev.mj31.logger.client.app.usecase.ingest.source

import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlinx.datetime.LocalDate

/**
 * Turns raw text lines into a [LogSource].
 *
 * Lines that do not match the format (stack traces, wrapped payloads) are appended to the previous
 * record instead of being dropped, so no information is lost. Lines appearing before the first
 * recognized record are the file's [LogSource.preamble], kept verbatim up to
 * [MAX_PREAMBLE_LINES]; anything beyond that is no longer a header but a file the format does not
 * fit, and is counted in [LogSource.skippedLineCount].
 *
 * The file is read in the zone it is handed, unless its lines carry an offset of their own: those
 * are read by it regardless, and the source then reports the first record's offset as its zone, so
 * that its times are shown the way the file writes them.
 */
class LogSourceAssembler(
    private val parserFactory: LogLineParserFactory,
) {

    fun assemble(
        descriptor: LogSourceDescriptor,
        spec: LogFormatSpec,
        lines: List<String>,
        referenceDate: LocalDate,
        zone: SourceZone = SourceZone.UTC,
    ): LogSource {
        val parsingSpec = if ((spec.zoneId ?: SourceZone.UTC_ID) == zone.id) spec else spec.withZoneId(zoneId = zone.id)
        val parser = parserFactory.create(spec = parsingSpec, referenceDate = referenceDate)
        val reading = Reading(sourceId = descriptor.id, expectedLines = lines.size)
        lines.forEachIndexed { index, line ->
            if (line.isBlank()) {
                reading.blank(line = line)
            } else {
                reading.accept(parsed = parser.parse(line = line), line = line, lineNumber = index + 1)
            }
        }
        reading.flush()

        return LogSource(
            id = descriptor.id,
            name = descriptor.name,
            path = descriptor.path,
            format = spec,
            entries = reading.entries,
            referenceDate = referenceDate,
            skippedLineCount = reading.skipped,
            preamble = reading.preamble.dropLastWhile { it.isBlank() },
            zone = reading.lineOffset?.let { SourceZone(id = SourceZone.offsetId(seconds = it), origin = ZoneOrigin.LINE) } ?: zone,
        )
    }

    /** What reading one file has gathered so far; bound to that file and thrown away after it. */
    private class Reading(private val sourceId: String, expectedLines: Int) {

        val entries = ArrayList<LogEntry>(expectedLines)
        val preamble = ArrayList<String>()
        var skipped = 0
            private set
        var lineOffset: Int? = null
            private set

        private val continuations = StringBuilder()
        private var continuationCount = 0
        private var pending: LogEntry? = null

        /**
         * A blank line inside a header separates its paragraphs; one after it separates records and
         * says nothing.
         */
        fun blank(line: String) {
            if (pending == null && preamble.isNotEmpty()) preamble += line
        }

        fun accept(parsed: ParsedLine, line: String, lineNumber: Int) {
            when (parsed) {
                is ParsedLine.Record -> record(parsed = parsed, line = line, lineNumber = lineNumber)
                is ParsedLine.ColumnHeader -> Unit
                is ParsedLine.Continuation -> continuation(text = parsed.text, line = line)
            }
        }

        fun flush() {
            val current = pending ?: return
            entries += if (continuations.isEmpty()) {
                current
            } else {
                current.copy(message = current.message + "\n" + continuations.toString().trimEnd())
            }
            continuations.clear()
            continuationCount = 0
        }

        private fun record(parsed: ParsedLine.Record, line: String, lineNumber: Int) {
            if (pending == null) lineOffset = parsed.utcOffsetSeconds
            flush()
            pending = LogEntry(
                id = "$sourceId:$lineNumber",
                sourceId = sourceId,
                lineNumber = lineNumber,
                timestamp = parsed.timestamp,
                level = parsed.level,
                tag = parsed.tag,
                message = parsed.message,
                rawLine = line,
            )
        }

        private fun continuation(text: String, line: String) {
            if (pending == null && preamble.size < MAX_PREAMBLE_LINES) {
                preamble += line
                return
            }
            // A run is cut off rather than followed forever: a file the format does not fit at all
            // produces nothing but continuations, and without a limit its first record would
            // swallow the whole file into a single unreadable entry. What is dropped is still
            // counted, so the source reports how much it could not read.
            if (pending == null || continuationCount >= MAX_CONTINUATION_LINES) {
                skipped++
            } else {
                continuations.appendLine(text)
                continuationCount++
            }
        }
    }

    private companion object {

        /** Long enough for any real stack trace, short enough that a misread file stays diagnosable. */
        const val MAX_CONTINUATION_LINES = 500

        /** Far more than any banner needs; past it the lines are a misfit format, not a header. */
        const val MAX_PREAMBLE_LINES = 100
    }
}
