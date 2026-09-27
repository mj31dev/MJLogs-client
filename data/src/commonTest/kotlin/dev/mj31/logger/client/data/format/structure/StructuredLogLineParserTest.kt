package dev.mj31.logger.client.data.format.structure

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.format.parse.DispatchingLogLineParserFactory
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.test.Test
import kotlinx.datetime.LocalDate

class StructuredLogLineParserTest {

    private val factory = DispatchingLogLineParserFactory()
    private val referenceDate = LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15)

    @Test
    fun `a JSON record is decomposed by key`() {
        val record = parse(
            spec = jsonSpec(),
            line = """{"timestamp":"2024-01-15T10:23:45.123","level":"WARN","logger":"Net","message":"slow"}""",
        ) as ParsedLine.Record

        assertThat(record.level).isEqualTo(LogLevel.WARN)
        assertThat(record.tag).isEqualTo("Net")
        assertThat(record.message).isEqualTo("slow")
    }

    @Test
    fun `a nested key is reachable through a dotted path`() {
        val spec = LogFormatSpec.Json(
            name = "nested",
            fields = RecordFieldMap.of(
                timestamp = ComponentLocator.Key(name = "@timestamp"),
                level = ComponentLocator.Key(name = "log.level"),
                message = ComponentLocator.Key(name = "message"),
            ),
            timestampPattern = "yyyy-MM-ddTHH:mm:ss.SSS",
        )

        val record = parse(
            spec = spec,
            line = """{"@timestamp":"2024-01-15T10:23:45.123","log":{"level":"ERROR"},"message":"down"}""",
        ) as ParsedLine.Record

        assertThat(record.level).isEqualTo(LogLevel.ERROR)
        assertThat(record.message).isEqualTo("down")
    }

    @Test
    fun `a line that is not an object continues the record before it`() {
        val parsed = parse(spec = jsonSpec(), line = "    at Socket.read(Socket.kt:42)")

        assertThat(parsed).isInstanceOf(ParsedLine.Continuation::class.java)
    }

    @Test
    fun `a quoted delimiter belongs to the field, not to the layout`() {
        val record = parse(
            spec = delimitedSpec(),
            line = """2024-01-15 10:23:45.123,INFO,"connected, then idle"""",
        ) as ParsedLine.Record

        assertThat(record.message).isEqualTo("connected, then idle")
    }

    @Test
    fun `a doubled quote inside a quoted field stands for one quote`() {
        val record = parse(
            spec = delimitedSpec(),
            line = """2024-01-15 10:23:45.123,INFO,"said ""stop"" twice"""",
        ) as ParsedLine.Record

        assertThat(record.message).isEqualTo("""said "stop" twice""")
    }

    @Test
    fun `a row whose quote is never closed is kept as text instead of being dropped`() {
        // This pipeline reads a file line by line, so a field carrying a newline cannot be assembled.
        // Reporting a continuation keeps the text attached to the previous record rather than losing it.
        val parsed = parse(spec = delimitedSpec(), line = """2024-01-15 10:23:45.123,INFO,"unterminated""")

        assertThat(parsed).isInstanceOf(ParsedLine.Continuation::class.java)
    }

    @Test
    fun `the header row is consumed rather than read as a record`() {
        val parser = parserFor(spec = delimitedSpec(hasHeader = true))

        val header = parser.parse(line = "timestamp,level,message")
        val first = parser.parse(line = "2024-01-15 10:23:45.123,INFO,connected")

        assertThat(header).isEqualTo(ParsedLine.ColumnHeader(names = listOf("timestamp", "level", "message")))
        assertThat(first).isInstanceOf(ParsedLine.Record::class.java)
    }

    @Test
    fun `lines before the header row are text rather than the header`() {
        val parser = parserFor(spec = delimitedSpec(hasHeader = true))

        val banner = parser.parse(line = "# exported by LogTool")
        val header = parser.parse(line = "timestamp,level,message")
        val first = parser.parse(line = "2024-01-15 10:23:45.123,INFO,connected")

        assertThat(banner).isInstanceOf(ParsedLine.Continuation::class.java)
        assertThat(header).isInstanceOf(ParsedLine.ColumnHeader::class.java)
        assertThat(first).isInstanceOf(ParsedLine.Record::class.java)
    }

    @Test
    fun `a header addressed by name is the row that names the timestamp column`() {
        val parser = parserFor(
            spec = delimitedSpec(hasHeader = true).copy(
                fields = RecordFieldMap.of(
                    timestamp = ComponentLocator.Key(name = "time"),
                    message = ComponentLocator.Key(name = "msg"),
                ),
            ),
        )

        val banner = parser.parse(line = "source,device,build")
        val header = parser.parse(line = "time,msg,extra")
        val first = parser.parse(line = "2024-01-15 10:23:45.123,connected,x")

        assertThat(banner).isInstanceOf(ParsedLine.Continuation::class.java)
        assertThat(header).isInstanceOf(ParsedLine.ColumnHeader::class.java)
        assertThat((first as ParsedLine.Record).message).isEqualTo("connected")
    }

    private fun jsonSpec(): LogFormatSpec.Json = LogFormatSpec.Json(
        name = "JSON Lines",
        fields = RecordFieldMap.of(
            timestamp = ComponentLocator.Key(name = "timestamp"),
            level = ComponentLocator.Key(name = "level"),
            tag = ComponentLocator.Key(name = "logger"),
            message = ComponentLocator.Key(name = "message"),
        ),
        timestampPattern = "yyyy-MM-ddTHH:mm:ss.SSS",
    )

    private fun delimitedSpec(hasHeader: Boolean = false): LogFormatSpec.Delimited = LogFormatSpec.Delimited(
        name = "Comma separated",
        delimiter = ',',
        hasHeader = hasHeader,
        fields = RecordFieldMap.of(
            timestamp = ComponentLocator.Index(position = 0),
            level = ComponentLocator.Index(position = 1),
            message = ComponentLocator.Index(position = 2),
        ),
        timestampPattern = "yyyy-MM-dd HH:mm:ss.SSS",
    )

    private fun parserFor(spec: LogFormatSpec): LogLineParser =
        factory.create(spec = spec, referenceDate = referenceDate)

    private fun parse(spec: LogFormatSpec, line: String): ParsedLine =
        parserFor(spec = spec).parse(line = line)
}
