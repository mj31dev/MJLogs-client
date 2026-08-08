package dev.mj31.logger.client.data.format.line

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory

class RegexLogLineParserTest {

    private val factory = RegexLogLineParserFactory()

    @Test
    fun `parses a complete record`() {
        val parser = parser(template = FULL_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val parsed = parser.parse(line = "2024-01-15 10:23:45.123 INFO [Network] Connected to server")

        val record = assertIs<ParsedLine.Record>(parsed)
        assertThat(record.timestamp).isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `maps every known level token`() {
        val parser = parser(template = FULL_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val warning = parser.parse(line = "2024-01-15 10:23:45.123 WARNING [Cache] Almost full")
        val fatal = parser.parse(line = "2024-01-15 10:23:46.123 wtf [Cache] Broken")

        assertThat(assertIs<ParsedLine.Record>(warning).level).isEqualTo(LogLevel.WARN)
        assertThat(assertIs<ParsedLine.Record>(fatal).level).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `returns a continuation for a line that does not match`() {
        val parser = parser(template = FULL_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val parsed = parser.parse(line = "    at com.example.Service.call(Service.kt:42)")

        assertThat(assertIs<ParsedLine.Continuation>(parsed).text).isEqualTo("at com.example.Service.call(Service.kt:42)")
    }

    @Test
    fun `returns a continuation when the timestamp values are invalid`() {
        val parser = parser(template = FULL_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val parsed = parser.parse(line = "2024-13-45 10:23:45.123 INFO [Network] Connected")

        assertThat(assertIs<ParsedLine.Continuation>(parsed).text).contains("2024-13-45")
    }

    @Test
    fun `falls back to the configured level when the pattern captures none`() {
        val parser = parser(
            template = MESSAGE_ONLY_TEMPLATE,
            timestampPattern = MILLIS_PATTERN,
            fallbackLevel = LogLevel.WARN,
        )

        val record = assertIs<ParsedLine.Record>(parser.parse(line = "2024-01-15 10:23:45.123 Something happened"))

        assertThat(record.level).isEqualTo(LogLevel.WARN)
        assertThat(record.tag).isEmpty()
        assertThat(record.message).isEqualTo("Something happened")
    }

    @Test
    fun `uses the rest of the line when the pattern captures no message`() {
        val parser = parser(template = PREFIX_ONLY_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val record = assertIs<ParsedLine.Record>(parser.parse(line = "2024-01-15 10:23:45.123 INFO   Connected to server"))

        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `extracts tag and message from a thread time layout`() {
        val parser = parser(template = THREADTIME_TEMPLATE, timestampPattern = MILLIS_PATTERN)

        val record = assertIs<ParsedLine.Record>(parser.parse(line = "2024-01-15 10:23:45.123 1234 1300 D Network : Connected"))

        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected")
    }

    @Test
    fun `keeps rollover state independent between parsers`() {
        val first = parser(template = MESSAGE_ONLY_TEMPLATE, timestampPattern = "HH:mm:ss")
        val second = parser(template = MESSAGE_ONLY_TEMPLATE, timestampPattern = "HH:mm:ss")

        val beforeMidnight = assertIs<ParsedLine.Record>(first.parse(line = "23:50:00 Closing"))
        val afterMidnight = assertIs<ParsedLine.Record>(first.parse(line = "00:05:00 Reopening"))
        val freshParser = assertIs<ParsedLine.Record>(second.parse(line = "00:05:00 Reopening"))

        assertThat(beforeMidnight.timestamp).isEqualTo(Instant.parse("2024-01-15T23:50:00Z"))
        assertThat(afterMidnight.timestamp).isEqualTo(Instant.parse("2024-01-16T00:05:00Z"))
        assertThat(freshParser.timestamp).isEqualTo(Instant.parse("2024-01-15T00:05:00Z"))
    }

    @Test
    fun `applies the offset declared by the specification`() {
        val parser = parser(template = MESSAGE_ONLY_TEMPLATE, timestampPattern = MILLIS_PATTERN, utcOffsetMinutes = 180)

        val record = assertIs<ParsedLine.Record>(parser.parse(line = "2024-01-15 10:23:45.123 Started"))

        assertThat(record.timestamp).isEqualTo(Instant.parse("2024-01-15T07:23:45.123Z"))
    }

    @Test
    fun `rejects a specification that cannot be compiled`() {
        val invalidLinePattern = LogFormatSpec(
            name = "broken",
            linePattern = "^([unbalanced",
            timestampPattern = MILLIS_PATTERN,
        )
        val invalidTimestampPattern = LogFormatSpec(
            name = "broken",
            linePattern = "^(?<ts>.*)\$",
            timestampPattern = "???",
        )

        val lineError = assertFailsWith<IllegalArgumentException> {
            factory.create(spec = invalidLinePattern, referenceDate = REFERENCE_DATE)
        }
        val timestampError = assertFailsWith<IllegalArgumentException> {
            factory.create(spec = invalidTimestampPattern, referenceDate = REFERENCE_DATE)
        }

        assertThat(lineError.message).contains("invalid line pattern")
        assertThat(timestampError.message).contains("invalid timestamp pattern")
    }

    private fun parser(
        template: String,
        timestampPattern: String,
        fallbackLevel: LogLevel = LogLevel.INFO,
        utcOffsetMinutes: Int = 0,
    ): LogLineParser {
        val timestamp = TimestampPatternCompiler.compile(pattern = timestampPattern)
        val spec = LogFormatSpec(
            name = "test format",
            linePattern = LineFormatCompiler.buildLinePattern(template = template, timestampRegex = timestamp.regexSource),
            timestampPattern = timestampPattern,
            fallbackLevel = fallbackLevel,
            utcOffsetMinutes = utcOffsetMinutes,
        )
        return factory.create(spec = spec, referenceDate = REFERENCE_DATE)
    }

    private companion object {
        const val MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
        const val FULL_TEMPLATE =
            "^\\s*%TS%\\s+(?<lvl>%LEVEL%)\\s+\\[?(?<tag>[\\w.-]{1,60})\\]?\\s*[:\\-]?\\s*(?<msg>.*)\$"
        const val MESSAGE_ONLY_TEMPLATE = "^\\s*%TS%\\s+(?<msg>.*)\$"
        const val PREFIX_ONLY_TEMPLATE = "^\\s*%TS%\\s+(?<lvl>%LEVEL%)"
        const val THREADTIME_TEMPLATE = "^\\s*%TS%\\s+\\d+\\s+\\d+\\s+(?<lvl>%LEVEL%)\\s+(?<tag>[^:]{1,60}?)\\s*:\\s?(?<msg>.*)\$"
        val REFERENCE_DATE = LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15)
    }
}
