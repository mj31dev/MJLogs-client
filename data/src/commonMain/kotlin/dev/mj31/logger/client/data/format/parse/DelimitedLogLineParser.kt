package dev.mj31.logger.client.data.format.parse

import dev.mj31.logger.client.data.format.timestamp.CompiledTimestampPattern
import dev.mj31.logger.client.data.format.timestamp.TimestampResolutionContext
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * Reads a row of delimiter separated columns, addressing components by header name or by ordinal.
 *
 * When the format declares a header, the row naming the columns is remembered rather than parsed,
 * which is what lets a [ComponentLocator.Key] be turned into a position. That row is not simply the
 * first one: an export often opens with a line or two about itself, so a row is taken for the header
 * only once it could be one — wide enough for every column the format addresses, and naming each
 * column it addresses by name. Rows before it are handed back as continuations, which is how a
 * preamble reaches the reader. Both the header and the previous timestamp are per-file state, so an
 * instance is bound to one file and is **not thread safe**.
 */
class DelimitedLogLineParser internal constructor(
    private val spec: LogFormatSpec.Delimited,
    private val timestamp: CompiledTimestampPattern,
    private val referenceDate: LocalDate,
) : LogLineParser {

    private var previousTimestamp: Instant? = null
    private val zone: TimeZone = spec.zoneId?.let { TimeZone.of(zoneId = it) } ?: TimeZone.UTC
    private var header: List<String>? = null
    private var headerConsumed: Boolean = !spec.hasHeader

    override fun parse(line: String): ParsedLine {
        val columns = DelimitedRow.split(line = line, delimiter = spec.delimiter)
            ?: return continuationOf(line = line)
        if (!headerConsumed) return headerOrText(line = line, columns = columns)
        return recordOf(columns = columns) ?: continuationOf(line = line)
    }

    private fun headerOrText(line: String, columns: List<String>): ParsedLine {
        val names = columns.map { it.trim() }
        if (!couldBeHeader(names = names)) return continuationOf(line = line)
        header = names
        headerConsumed = true
        return ParsedLine.ColumnHeader(names = names)
    }

    private fun recordOf(columns: List<String>): ParsedLine.Record? {
        val rawTimestamp = valueOf(columns = columns, locator = spec.fields.timestamp) ?: return null
        val context = TimestampResolutionContext(
            referenceDate = referenceDate,
            zone = zone,
            previous = previousTimestamp,
        )
        val resolved = timestamp.resolve(text = rawTimestamp, context = context) ?: return null
        previousTimestamp = resolved

        return ParsedLine.Record(
            utcOffsetSeconds = timestamp.explicitOffsetSeconds(text = rawTimestamp),
            timestamp = resolved,
            level = levelOf(columns = columns),
            tag = textOf(columns = columns, component = LogComponent.TAG),
            message = textOf(columns = columns, component = LogComponent.MESSAGE),
        )
    }

    private fun couldBeHeader(names: List<String>): Boolean {
        val locators = LogComponent.entries.mapNotNull { spec.fields[it] }
        return locators.all { locator ->
            when (locator) {
                is ComponentLocator.Index -> locator.position < names.size
                is ComponentLocator.Key -> names.any { it.equals(other = locator.name, ignoreCase = true) }
            }
        }
    }

    private fun continuationOf(line: String): ParsedLine.Continuation = ParsedLine.Continuation(text = line.trim())

    private fun levelOf(columns: List<String>): LogLevel {
        val token = textOf(columns = columns, component = LogComponent.LEVEL)
        if (token.isEmpty()) return spec.fallbackLevel
        return LogLevel.fromToken(token = token) ?: spec.fallbackLevel
    }

    private fun textOf(columns: List<String>, component: LogComponent): String {
        val locator = spec.fields[component] ?: return ""
        return valueOf(columns = columns, locator = locator).orEmpty()
    }

    private fun valueOf(columns: List<String>, locator: ComponentLocator): String? {
        val position = positionOf(locator = locator) ?: return null
        return columns.getOrNull(index = position)?.trim()
    }

    private fun positionOf(locator: ComponentLocator): Int? = when (locator) {
        is ComponentLocator.Index -> locator.position
        is ComponentLocator.Key -> header
            ?.indexOfFirst { it.equals(other = locator.name, ignoreCase = true) }
            ?.takeIf { it >= 0 }
    }
}
