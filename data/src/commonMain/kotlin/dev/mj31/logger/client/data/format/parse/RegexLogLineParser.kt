package dev.mj31.logger.client.data.format.parse

import dev.mj31.logger.client.data.format.timestamp.TimestampResolutionContext
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.data.format.line.CompiledLineFormat

/**
 * Regex based [LogLineParser].
 *
 * An instance is bound to a single file: it remembers the timestamp of the previous record so that
 * time-only formats stay monotonic across midnight. Instances are therefore **not thread safe** and
 * must never be shared between files or between concurrent readers.
 */
class RegexLogLineParser internal constructor(
    private val format: CompiledLineFormat,
    private val referenceDate: LocalDate,
) : LogLineParser {

    private var previousTimestamp: Instant? = null

    override fun parse(line: String): ParsedLine {
        val match = format.lineRegex.find(input = line) ?: return continuationOf(line = line)
        val context = TimestampResolutionContext(
            referenceDate = referenceDate,
            utcOffsetMinutes = format.spec.utcOffsetMinutes,
            previous = previousTimestamp,
        )
        val timestamp = format.timestamp.resolve(match = match, context = context) ?: return continuationOf(line = line)
        previousTimestamp = timestamp
        return ParsedLine.Record(
            timestamp = timestamp,
            level = levelOf(match = match),
            tag = tagOf(match = match),
            message = messageOf(match = match, line = line),
        )
    }

    private fun continuationOf(line: String): ParsedLine.Continuation = ParsedLine.Continuation(text = line.trim())

    private fun levelOf(match: MatchResult): LogLevel {
        if (!format.hasLevelGroup) return format.spec.fallbackLevel
        val token = match.groups[LogFormatGroups.LEVEL]?.value ?: return format.spec.fallbackLevel
        return LogLevel.fromToken(token = token) ?: format.spec.fallbackLevel
    }

    private fun tagOf(match: MatchResult): String {
        if (!format.hasTagGroup) return ""
        return match.groups[LogFormatGroups.TAG]?.value?.trim().orEmpty()
    }

    private fun messageOf(match: MatchResult, line: String): String {
        if (format.hasMessageGroup) return match.groups[LogFormatGroups.MESSAGE]?.value.orEmpty()
        val end = match.range.last + 1
        return if (end >= line.length) "" else line.substring(startIndex = end).trim()
    }
}
