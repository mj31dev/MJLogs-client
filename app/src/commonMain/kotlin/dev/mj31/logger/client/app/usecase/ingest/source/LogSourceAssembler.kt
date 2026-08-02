package dev.mj31.logger.client.app.usecase.ingest.source

import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlinx.datetime.LocalDate

/**
 * Turns raw text lines into a [LogSource].
 *
 * Lines that do not match the format (stack traces, wrapped payloads) are appended to the previous
 * record instead of being dropped, so no information is lost. Lines appearing before the first
 * recognized record are counted in [LogSource.skippedLineCount].
 */
class LogSourceAssembler(
    private val parserFactory: LogLineParserFactory,
) {

    fun assemble(
        descriptor: LogSourceDescriptor,
        spec: LogFormatSpec,
        lines: List<String>,
        referenceDate: LocalDate,
    ): LogSource {
        val parser = parserFactory.create(spec = spec, referenceDate = referenceDate)
        val entries = ArrayList<LogEntry>(lines.size)
        val continuations = StringBuilder()
        var pending: LogEntry? = null
        var skipped = 0

        fun flush() {
            val current = pending ?: return
            entries += if (continuations.isEmpty()) {
                current
            } else {
                current.copy(message = current.message + "\n" + continuations.toString().trimEnd())
            }
            continuations.clear()
        }

        lines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            when (val parsed = parser.parse(line = line)) {
                is ParsedLine.Record -> {
                    flush()
                    pending = parsed.toEntry(
                        descriptor = descriptor,
                        lineNumber = index + 1,
                        rawLine = line,
                    )
                }

                is ParsedLine.Continuation -> {
                    if (pending == null) {
                        skipped++
                    } else {
                        continuations.appendLine(parsed.text)
                    }
                }
            }
        }
        flush()

        return LogSource(
            id = descriptor.id,
            name = descriptor.name,
            path = descriptor.path,
            format = spec,
            entries = entries,
            skippedLineCount = skipped,
        )
    }

    private fun ParsedLine.Record.toEntry(
        descriptor: LogSourceDescriptor,
        lineNumber: Int,
        rawLine: String,
    ): LogEntry = LogEntry(
        id = "${descriptor.id}:$lineNumber",
        sourceId = descriptor.id,
        lineNumber = lineNumber,
        timestamp = timestamp,
        level = level,
        tag = tag,
        message = message,
        rawLine = rawLine,
    )
}
