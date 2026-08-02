package dev.mj31.logger.client.app.fake.format

import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlinx.datetime.Instant

/** Parser produced by [ScriptedLogLineParserFactory]; stateless and therefore reusable per line. */
class ScriptedLogLineParser(
    private val script: Map<String, ParsedLine> = emptyMap(),
) : LogLineParser {

    override fun parse(line: String): ParsedLine = script[line] ?: parseByConvention(line = line)

    private fun parseByConvention(line: String): ParsedLine {
        val parts = line.split(FIELD_SEPARATOR)
        val epochMillis = parts.getOrNull(index = 0)?.trim()?.toLongOrNull()
        val level = parts.getOrNull(index = 1)?.let { token -> LogLevel.fromToken(token = token) }
        return if (parts.size < FIELD_COUNT || epochMillis == null || level == null) {
            ParsedLine.Continuation(text = line)
        } else {
            ParsedLine.Record(
                timestamp = Instant.fromEpochMilliseconds(epochMilliseconds = epochMillis),
                level = level,
                tag = parts[2].trim(),
                message = parts.drop(n = FIELD_COUNT - 1).joinToString(separator = FIELD_SEPARATOR),
            )
        }
    }

    companion object {

        const val FIELD_SEPARATOR: String = "|"
        const val FIELD_COUNT: Int = 4

        /** Renders a line the convention based parser turns into a [ParsedLine.Record]. */
        fun recordLine(
            timestamp: Instant,
            level: LogLevel = LogLevel.INFO,
            tag: String = "Network",
            message: String = "Connected",
        ): String = listOf(
            timestamp.toEpochMilliseconds().toString(),
            level.name,
            tag,
            message,
        ).joinToString(separator = FIELD_SEPARATOR)
    }
}
