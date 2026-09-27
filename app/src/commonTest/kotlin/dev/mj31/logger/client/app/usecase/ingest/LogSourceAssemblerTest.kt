package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import dev.mj31.logger.client.domain.model.time.SourceZone
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParserFactory
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParser
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceDescriptor
import dev.mj31.logger.client.domain.format.parse.ParsedLine

class LogSourceAssemblerTest {

    private val descriptor = LogSourceDescriptor(id = "src-a", name = "app.txt", path = "/logs/app.txt")
    private val referenceDate = LocalDate(year = 2024, monthNumber = 5, dayOfMonth = 1)

    private fun assemble(lines: List<String>): LogSource =
        LogSourceAssembler(parserFactory = ScriptedLogLineParserFactory()).assemble(
            descriptor = descriptor,
            spec = TestLogEntries.SPEC,
            lines = lines,
            referenceDate = referenceDate,
        )

    private fun recordLine(offsetMillis: Long, level: LogLevel = LogLevel.INFO, message: String = "Connected"): String =
        ScriptedLogLineParser.recordLine(
            timestamp = TestLogEntries.at(offsetMillis = offsetMillis),
            level = level,
            tag = "Network",
            message = message,
        )

    @Test
    fun `records become entries carrying identity line number and source`() {
        val lines = listOf(
            recordLine(offsetMillis = 0L, message = "Start"),
            recordLine(offsetMillis = 1_000L, level = LogLevel.ERROR, message = "Boom"),
        )

        val source = assemble(lines = lines)

        assertThat(source.entryCount).isEqualTo(2)
        assertThat(source.entries.map { it.id }).containsExactly("src-a:1", "src-a:2").inOrder()
        assertThat(source.entries.map { it.lineNumber }).containsExactly(1, 2).inOrder()
        assertThat(source.entries.map { it.sourceId }).containsExactly("src-a", "src-a")
        assertThat(source.entries[0].timestamp).isEqualTo(TestLogEntries.at(offsetMillis = 0L))
        assertThat(source.entries[1].level).isEqualTo(LogLevel.ERROR)
        assertThat(source.entries[1].tag).isEqualTo("Network")
        assertThat(source.entries[1].message).isEqualTo("Boom")
        assertThat(source.entries[1].rawLine).isEqualTo(lines[1])
        assertThat(source.skippedLineCount).isEqualTo(0)
    }

    @Test
    fun `source keeps the descriptor identity and the spec it was parsed with`() {
        val source = assemble(lines = listOf(recordLine(offsetMillis = 0L)))

        assertThat(source.id).isEqualTo("src-a")
        assertThat(source.name).isEqualTo("app.txt")
        assertThat(source.path).isEqualTo("/logs/app.txt")
        assertThat(source.format).isEqualTo(TestLogEntries.SPEC)
    }

    @Test
    fun `the parser is created once per file with the spec and the reference date`() {
        val factory = ScriptedLogLineParserFactory()

        LogSourceAssembler(parserFactory = factory).assemble(
            descriptor = descriptor,
            spec = TestLogEntries.SPEC,
            lines = listOf(recordLine(offsetMillis = 0L), recordLine(offsetMillis = 1_000L)),
            referenceDate = referenceDate,
        )

        assertThat(factory.createdSpecs).containsExactly(TestLogEntries.SPEC)
        assertThat(factory.lastReferenceDate).isEqualTo(referenceDate)
    }

    @Test
    fun `a continuation line is appended to the previous record separated by a line break`() {
        val source = assemble(
            lines = listOf(
                recordLine(offsetMillis = 0L, level = LogLevel.ERROR, message = "Boom"),
                "\tat Foo.kt:12",
            ),
        )

        assertThat(source.entryCount).isEqualTo(1)
        assertThat(source.entries[0].message).isEqualTo("Boom\n\tat Foo.kt:12")
    }

    @Test
    fun `several continuation lines are appended in order`() {
        val source = assemble(
            lines = listOf(
                recordLine(offsetMillis = 0L, level = LogLevel.ERROR, message = "Boom"),
                "\tat Foo.kt:12",
                "\tat Bar.kt:34",
                recordLine(offsetMillis = 1_000L, message = "Recovered"),
            ),
        )

        assertThat(source.entryCount).isEqualTo(2)
        assertThat(source.entries[0].message).isEqualTo("Boom\n\tat Foo.kt:12\n\tat Bar.kt:34")
        assertThat(source.entries[1].message).isEqualTo("Recovered")
        assertThat(source.entries[1].lineNumber).isEqualTo(4)
        assertThat(source.skippedLineCount).isEqualTo(0)
    }

    @Test
    fun `a continuation attached to the last record is still flushed`() {
        val source = assemble(
            lines = listOf(
                recordLine(offsetMillis = 0L, message = "Head"),
                "trailing detail",
            ),
        )

        assertThat(source.entries.single().message).isEqualTo("Head\ntrailing detail")
    }

    @Test
    fun `lines before the first record are the preamble rather than skipped lines`() {
        val source = assemble(
            lines = listOf(
                "### log file header ###",
                "generated by tooling",
                recordLine(offsetMillis = 0L, message = "Start"),
                "\tdetail of the first record",
            ),
        )

        assertThat(source.preamble).containsExactly("### log file header ###", "generated by tooling").inOrder()
        assertThat(source.skippedLineCount).isEqualTo(0)
        assertThat(source.entryCount).isEqualTo(1)
        assertThat(source.entries[0].lineNumber).isEqualTo(3)
        assertThat(source.entries[0].message).isEqualTo("Start\n\tdetail of the first record")
    }

    @Test
    fun `blank lines are skipped entirely and do not shift line numbers`() {
        val source = assemble(
            lines = listOf(
                "",
                recordLine(offsetMillis = 0L, message = "First"),
                "   ",
                recordLine(offsetMillis = 1_000L, message = "Second"),
                "",
            ),
        )

        assertThat(source.entries.map { it.lineNumber }).containsExactly(2, 4).inOrder()
        assertThat(source.entries.map { it.id }).containsExactly("src-a:2", "src-a:4").inOrder()
        assertThat(source.entries.map { it.message }).containsExactly("First", "Second").inOrder()
        assertThat(source.skippedLineCount).isEqualTo(0)
    }

    @Test
    fun `a file made only of unparseable lines produces no entries`() {
        val source = assemble(lines = listOf("header", "another header"))

        assertThat(source.entries).isEmpty()
        assertThat(source.preamble).containsExactly("header", "another header").inOrder()
        assertThat(source.skippedLineCount).isEqualTo(0)
        assertThat(source.timeRange).isNull()
    }

    @Test
    fun `the preamble keeps its inner blank lines verbatim but not the ones before the first record`() {
        val source = assemble(
            lines = listOf(
                "",
                "Device: Pixel 8",
                "",
                "  Build: 1.4.2  ",
                "",
                recordLine(offsetMillis = 0L, message = "Start"),
            ),
        )

        assertThat(source.preamble).containsExactly("Device: Pixel 8", "", "  Build: 1.4.2  ").inOrder()
    }

    @Test
    fun `text after the first record is never preamble`() {
        val source = assemble(
            lines = listOf(
                recordLine(offsetMillis = 0L, message = "Start"),
                "detail",
            ),
        )

        assertThat(source.preamble).isEmpty()
    }

    @Test
    fun `a preamble longer than a header can be is cut off and the rest counted as skipped`() {
        val junk = List(size = 130) { index -> "junk $index" }

        val source = assemble(lines = junk + recordLine(offsetMillis = 0L, message = "Start"))

        assertThat(source.preamble).hasSize(100)
        assertThat(source.preamble.first()).isEqualTo("junk 0")
        assertThat(source.skippedLineCount).isEqualTo(30)
        assertThat(source.entryCount).isEqualTo(1)
    }

    @Test
    fun `the row naming the columns belongs to the format and not to the preamble`() {
        val source = LogSourceAssembler(
            parserFactory = ScriptedLogLineParserFactory(
                script = mapOf("time,level,message" to ParsedLine.ColumnHeader(names = listOf("time", "level", "message"))),
            ),
        ).assemble(
            descriptor = descriptor,
            spec = TestLogEntries.SPEC,
            lines = listOf("# exported by tool", "time,level,message", recordLine(offsetMillis = 0L)),
            referenceDate = referenceDate,
        )

        assertThat(source.preamble).containsExactly("# exported by tool")
        assertThat(source.skippedLineCount).isEqualTo(0)
        assertThat(source.entryCount).isEqualTo(1)
    }

    @Test
    fun `an empty file produces an empty source`() {
        val source = assemble(lines = emptyList())

        assertThat(source.entries).isEmpty()
        assertThat(source.skippedLineCount).isEqualTo(0)
    }

    @Test
    fun `a file is read in the zone it is handed and reports it`() {
        val factory = ScriptedLogLineParserFactory()
        val zone = SourceZone(id = "Europe/Berlin", origin = ZoneOrigin.HEADER)

        val source = LogSourceAssembler(parserFactory = factory).assemble(
            descriptor = descriptor,
            spec = TestLogEntries.SPEC,
            lines = listOf(recordLine(offsetMillis = 0L)),
            referenceDate = referenceDate,
            zone = zone,
        )

        assertThat(factory.createdSpecs.single().zoneId).isEqualTo("Europe/Berlin")
        assertThat(source.zone).isEqualTo(zone)
        assertThat(source.format).isEqualTo(TestLogEntries.SPEC)
    }

    @Test
    fun `lines that carry their own offset report it as the zone of the file`() {
        val line = recordLine(offsetMillis = 0L)
        val source = LogSourceAssembler(
            parserFactory = ScriptedLogLineParserFactory(
                script = mapOf(
                    line to ParsedLine.Record(
                        timestamp = TestLogEntries.at(offsetMillis = 0L),
                        level = LogLevel.INFO,
                        tag = "Network",
                        message = "Connected",
                        utcOffsetSeconds = 10_800,
                    ),
                ),
            ),
        ).assemble(
            descriptor = descriptor,
            spec = TestLogEntries.SPEC,
            lines = listOf(line),
            referenceDate = referenceDate,
            zone = SourceZone(id = "Asia/Tokyo", origin = ZoneOrigin.CHOSEN),
        )

        assertThat(source.zone).isEqualTo(SourceZone(id = "UTC+03:00", origin = ZoneOrigin.LINE))
    }
}
