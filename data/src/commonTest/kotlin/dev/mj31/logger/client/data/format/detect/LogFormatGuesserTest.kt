package dev.mj31.logger.client.data.format.detect

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory

class LogFormatGuesserTest {

    private val guesser = LogFormatGuesser()
    private val compiler = TemplateLogFormatCompiler()
    private val factory = RegexLogLineParserFactory()

    @Test
    fun `infers a layout no built-in candidate covers`() {
        val lines = listOf(
            "<0000>~01.08.2026_10.23.45~ANALYTICS~event dispatched (0)",
            "<0001>~01.08.2026_10.23.46~ANALYTICS~event dispatched (1)",
            "<0002>~01.08.2026_10.23.47~ANALYTICS~event dispatched (2)",
        )

        val guess = requireNotNull(guesser.guess(sampleLines = lines))

        assertThat(guess.timestampPattern).isEqualTo("dd.MM.yyyy_HH.mm.ss")
        assertThat(guess.structureTemplate).isEqualTo("<{any}>~{timestamp}~{tag}~{message}")

        val record = parse(guess = guess, line = lines.first())
        assertThat(record.tag).isEqualTo("ANALYTICS")
        assertThat(record.message).isEqualTo("event dispatched (0)")
    }

    @Test
    fun `recognizes a level and a tag around the timestamp`() {
        val lines = listOf(
            "2024/01/15 10:23:45.123 :: WARN :: CacheStore :: evicted 15 entries",
            "2024/01/15 10:23:46.456 :: ERROR :: Network :: connection reset",
            "2024/01/15 10:23:47.789 :: INFO :: CacheStore :: warm up done",
        )

        val guess = requireNotNull(guesser.guess(sampleLines = lines))

        assertThat(guess.timestampPattern).isEqualTo("yyyy/MM/dd HH:mm:ss.SSS")
        assertThat(guess.structureTemplate).isEqualTo("{timestamp} :: {level} :: {tag} :: {message}")

        val record = parse(guess = guess, line = lines[1])
        assertThat(record.level).isEqualTo(LogLevel.ERROR)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("connection reset")
    }

    @Test
    fun `uses the samples to tell a day from a month`() {
        val dayFirst = listOf(
            "25/01/2024 10:23:45 boot",
            "26/01/2024 10:23:46 ready",
            "27/01/2024 10:23:47 idle",
        )

        val guess = requireNotNull(guesser.guess(sampleLines = dayFirst))

        assertThat(guess.timestampPattern).isEqualTo("dd/MM/yyyy HH:mm:ss")
    }

    @Test
    fun `keeps a varying prefix out of the captured fields`() {
        val lines = listOf(
            "worker-1 | 10:23:45 | started",
            "worker-27 | 10:23:46 | polling",
            "worker-3 | 10:23:47 | stopped",
        )

        val guess = requireNotNull(guesser.guess(sampleLines = lines))

        assertThat(guess.timestampPattern).isEqualTo("HH:mm:ss")
        assertThat(guess.structureTemplate).startsWith("worker-{any}")

        val record = parse(guess = guess, line = lines[1])
        assertThat(record.message).isEqualTo("polling")
    }

    @Test
    fun `returns nothing when there is no timestamp at all`() {
        val guess = guesser.guess(
            sampleLines = listOf(
                "starting the exporter",
                "everything is fine",
                "shutting down",
            ),
        )

        assertThat(guess).isNull()
    }

    @Test
    fun `returns nothing for a single line`() {
        assertThat(guesser.guess(sampleLines = listOf("10:23:45 boot"))).isNull()
    }

    private fun parse(guess: ManualFormatInput, line: String): ParsedLine.Record {
        val spec = assertIs<FormatCompilationResult.Success>(compiler.compile(input = guess)).spec
        val parser = factory.create(spec = spec, referenceDate = REFERENCE_DATE)
        return assertIs<ParsedLine.Record>(parser.parse(line = line))
    }

    private companion object {
        val REFERENCE_DATE = LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15)
    }
}
