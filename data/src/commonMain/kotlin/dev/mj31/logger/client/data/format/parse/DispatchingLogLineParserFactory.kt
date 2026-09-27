package dev.mj31.logger.client.data.format.parse

import dev.mj31.logger.client.data.format.line.CompiledLineFormat
import dev.mj31.logger.client.data.format.timestamp.CompiledTimestampPattern
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlinx.datetime.LocalDate

/**
 * Picks the parser that matches the variant of a specification.
 *
 * Compilation — of the line regex, of the timestamp pattern — is the expensive part of reading a
 * file and happens here, once, rather than per line.
 */
class DispatchingLogLineParserFactory : LogLineParserFactory {

    override fun create(spec: LogFormatSpec, referenceDate: LocalDate): LogLineParser = when (spec) {
        is LogFormatSpec.Regex -> RegexLogLineParser(
            format = CompiledLineFormat.compile(spec = spec),
            referenceDate = referenceDate,
        )

        is LogFormatSpec.Json -> JsonLogLineParser(
            spec = spec,
            timestamp = timestampOf(spec = spec),
            referenceDate = referenceDate,
        )

        is LogFormatSpec.Delimited -> DelimitedLogLineParser(
            spec = spec,
            timestamp = timestampOf(spec = spec),
            referenceDate = referenceDate,
        )
    }

    private fun timestampOf(spec: LogFormatSpec): CompiledTimestampPattern =
        runCatching { TimestampPatternCompiler.compile(pattern = spec.timestampPattern) }
            .getOrElse { error ->
                throw IllegalArgumentException(
                    "Log format '${spec.name}' has an invalid timestamp pattern: ${error.message}",
                )
            }
}
