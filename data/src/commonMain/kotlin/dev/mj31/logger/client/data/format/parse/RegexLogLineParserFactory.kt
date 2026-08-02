package dev.mj31.logger.client.data.format.parse

import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.data.format.line.CompiledLineFormat

/** Creates [RegexLogLineParser] instances; every specification is compiled exactly once per call. */
class RegexLogLineParserFactory : LogLineParserFactory {

    override fun create(spec: LogFormatSpec, referenceDate: LocalDate): LogLineParser =
        RegexLogLineParser(format = CompiledLineFormat.compile(spec = spec), referenceDate = referenceDate)
}
