package dev.mj31.logger.client.app.fake.format

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlinx.datetime.LocalDate

/**
 * Deterministic replacement for the regex based parser of the data layer.
 *
 * A line is recognized as a record when it follows `epochMillis|LEVEL|tag|message`
 * (see [ScriptedLogLineParser.recordLine]); every other non-blank line is reported as a
 * continuation. Tests can additionally pin single lines through [script].
 */
class ScriptedLogLineParserFactory(
    private val script: Map<String, ParsedLine> = emptyMap(),
    private val createFailureMessage: String? = null,
) : LogLineParserFactory {

    private val mutableCreatedSpecs = mutableListOf<LogFormatSpec>()

    val createdSpecs: List<LogFormatSpec>
        get() = mutableCreatedSpecs.toList()

    var lastReferenceDate: LocalDate? = null
        private set

    override fun create(spec: LogFormatSpec, referenceDate: LocalDate): LogLineParser {
        createFailureMessage?.let { message -> throw IllegalArgumentException(message) }
        mutableCreatedSpecs += spec
        lastReferenceDate = referenceDate
        return ScriptedLogLineParser(script = script)
    }
}
