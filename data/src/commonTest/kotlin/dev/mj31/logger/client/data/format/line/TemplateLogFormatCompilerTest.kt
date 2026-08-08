package dev.mj31.logger.client.data.format.line

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory

class TemplateLogFormatCompilerTest {

    private val compiler = TemplateLogFormatCompiler()
    private val factory = RegexLogLineParserFactory()

    @Test
    fun `compiles a full template and parses a matching line`() {
        val spec = compiled(
            timestampPattern = "yyyy-MM-dd HH:mm:ss.SSS",
            structureTemplate = "{timestamp} {level} {tag}: {message}",
        )

        val record = record(spec = spec, line = "2024-01-15 10:23:45.123 INFO Network: Connected to server")

        assertThat(spec.name).isEqualTo("Custom format")
        assertThat(spec.origin).isEqualTo(FormatOrigin.USER_DEFINED)
        assertThat(record.timestamp).isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `keeps the configured offset`() {
        val spec = compiled(
            timestampPattern = "yyyy-MM-dd HH:mm:ss",
            structureTemplate = "{timestamp} {message}",
            utcOffsetMinutes = 180,
        )

        val record = record(spec = spec, line = "2024-01-15 10:23:45 Started")

        assertThat(spec.utcOffsetMinutes).isEqualTo(180)
        assertThat(record.timestamp).isEqualTo(Instant.parse("2024-01-15T07:23:45Z"))
        assertThat(record.message).isEqualTo("Started")
    }

    @Test
    fun `matches literal separators and tolerates wider whitespace`() {
        val spec = compiled(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "[{timestamp}] ({level}) {message}",
        )

        val record = record(spec = spec, line = "[10:23:45]    (WARNING)   Disk almost full")

        assertThat(record.level).isEqualTo(LogLevel.WARN)
        assertThat(record.message).isEqualTo("Disk almost full")
    }

    @Test
    fun `matches regex metacharacters as plain literals`() {
        val spec = compiled(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "#{timestamp}# | ({level}) [*] {tag} -> {message}",
        )

        val record = record(spec = spec, line = "#10:23:45# | (WARN) [*] Cache -> evicted 15 entries")

        assertThat(record.level).isEqualTo(LogLevel.WARN)
        assertThat(record.tag).isEqualTo("Cache")
        assertThat(record.message).isEqualTo("evicted 15 entries")
    }

    @Test
    fun `a literal that looks like a placeholder is rejected with a readable message`() {
        val failure = failure(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "{timestamp} {\"level\":\"INFO\"} {message}",
        )

        assertThat(failure.message).contains("Unknown placeholder")
        assertThat(failure.message).contains("{{")
    }

    @Test
    fun `doubled braces match a literal brace`() {
        val spec = compiled(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "{timestamp} {{\"level\":\"{level}\"}} {message}",
        )

        val record = record(spec = spec, line = "10:23:45 {\"level\":\"ERROR\"} upload failed")

        assertThat(record.level).isEqualTo(LogLevel.ERROR)
        assertThat(record.message).isEqualTo("upload failed")
    }

    @Test
    fun `doubled braces stay literal next to a placeholder`() {
        val spec = compiled(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "{{{timestamp}}} {message}",
        )

        val record = record(spec = spec, line = "{10:23:45} cache warmed")

        assertThat(record.message).isEqualTo("cache warmed")
    }

    @Test
    fun `discards a varying prefix through the any placeholder`() {
        val spec = compiled(
            timestampPattern = "dd.MM.yyyy_HH.mm.ss",
            structureTemplate = "<{any}>~{timestamp}~{tag}~{message}",
        )

        val record = record(spec = spec, line = "<0042>~15.01.2024_10.23.45~ANALYTICS~event dispatched (42)")

        assertThat(record.timestamp).isEqualTo(Instant.parse("2024-01-15T10:23:45Z"))
        assertThat(record.tag).isEqualTo("ANALYTICS")
        assertThat(record.message).isEqualTo("event dispatched (42)")
    }

    @Test
    fun `the any placeholder may be repeated`() {
        val spec = compiled(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "[{any}] {timestamp} ({any}) {level} {message}",
        )

        val record = record(spec = spec, line = "[worker-3] 10:23:45 (pid 8172) ERROR Upload failed")

        assertThat(record.level).isEqualTo(LogLevel.ERROR)
        assertThat(record.message).isEqualTo("Upload failed")
    }

    @Test
    fun `fails when the template has no timestamp placeholder`() {
        val failure = failure(timestampPattern = "HH:mm:ss", structureTemplate = "{level} {tag}: {message}")

        assertThat(failure.message).contains("{timestamp}")
    }

    @Test
    fun `fails for an unknown placeholder`() {
        val failure = failure(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "{timestamp} {thread} {message}",
        )

        assertThat(failure.message).contains("{thread}")
        assertThat(failure.message).contains("{timestamp}")
    }

    @Test
    fun `fails for a duplicated placeholder`() {
        val failure = failure(
            timestampPattern = "HH:mm:ss",
            structureTemplate = "{timestamp} {message} {message}",
        )

        assertThat(failure.message).contains("more than once")
    }

    @Test
    fun `fails for an unusable timestamp pattern`() {
        val failure = failure(timestampPattern = "???", structureTemplate = "{timestamp} {message}")

        assertThat(failure.message).contains("no known token")
        assertThat(failure.field).isEqualTo(FormatErrorField.TIMESTAMP_PATTERN)
    }

    @Test
    fun `every structural failure points at the structure template`() {
        val failures = listOf(
            failure(timestampPattern = "HH:mm:ss", structureTemplate = "{level} {message}"),
            failure(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {thread}"),
            failure(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message} {message}"),
        )

        assertThat(failures.map { it.field })
            .containsExactlyElementsIn(List(size = failures.size) { FormatErrorField.STRUCTURE_TEMPLATE })
    }

    private fun compiled(
        timestampPattern: String,
        structureTemplate: String,
        utcOffsetMinutes: Int = 0,
    ): LogFormatSpec {
        val result = compiler.compile(
            input = ManualFormatInput(
                timestampPattern = timestampPattern,
                structureTemplate = structureTemplate,
                utcOffsetMinutes = utcOffsetMinutes,
            ),
        )
        return assertIs<FormatCompilationResult.Success>(result).spec
    }

    private fun failure(timestampPattern: String, structureTemplate: String): FormatCompilationResult.Failure {
        val result = compiler.compile(
            input = ManualFormatInput(timestampPattern = timestampPattern, structureTemplate = structureTemplate),
        )
        return assertIs<FormatCompilationResult.Failure>(result)
    }

    private fun record(spec: LogFormatSpec, line: String): ParsedLine.Record {
        val parser = factory.create(spec = spec, referenceDate = REFERENCE_DATE)
        return assertIs<ParsedLine.Record>(parser.parse(line = line))
    }

    private companion object {
        val REFERENCE_DATE = LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15)
    }
}
