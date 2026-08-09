package dev.mj31.logger.client.data.format.timestamp

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TimestampPatternCompilerTest {

    @Test
    fun `compiles a full date and time pattern`() {
        val compiled = TimestampPatternCompiler.compile(pattern = "yyyy-MM-dd HH:mm:ss.SSS")

        assertThat(compiled.pattern).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS")
        assertThat(compiled.fields).containsExactly(
            TimestampField.YEAR,
            TimestampField.MONTH,
            TimestampField.DAY,
            TimestampField.HOUR,
            TimestampField.MINUTE,
            TimestampField.SECOND,
            TimestampField.FRACTION,
        )
        assertThat(compiled.regexSource).contains("(?<tsYear>\\d{4})")
        assertThat(compiled.regexSource).doesNotContain("^")
    }

    @Test
    fun `resolves a full date and time`() {
        val resolved = resolve(pattern = "yyyy-MM-dd HH:mm:ss.SSS", text = "2024-01-15 10:23:45.123")

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
    }

    @Test
    fun `resolves a two digit year relative to year 2000`() {
        val resolved = resolve(pattern = "yy-MM-dd HH:mm:ss", text = "24-01-15 10:23:45")

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T10:23:45Z"))
    }

    @Test
    fun `resolves textual month names ignoring case`() {
        val resolved = resolve(pattern = "dd/MMM/yyyy:HH:mm:ss", text = "15/jan/2024:10:23:45")

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T10:23:45Z"))
    }

    @Test
    fun `resolves millisecond and microsecond fractions`() {
        val millis = resolve(pattern = "yyyy-MM-dd HH:mm:ss,SSS", text = "2024-01-15 10:23:45,007")
        val micros = resolve(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS", text = "2024-01-15 10:23:45.123456")

        assertThat(millis).isEqualTo(Instant.parse("2024-01-15T10:23:45.007Z"))
        assertThat(micros).isEqualTo(Instant.parse("2024-01-15T10:23:45.123456Z"))
    }

    @Test
    fun `resolves explicit offsets`() {
        val pattern = "yyyy-MM-ddTHH:mm:ss.SSSXXX"
        val expected = Instant.parse("2024-01-15T07:23:45.123Z")

        assertThat(resolve(pattern = pattern, text = "2024-01-15T10:23:45.123+03:00")).isEqualTo(expected)
        assertThat(resolve(pattern = pattern, text = "2024-01-15T10:23:45.123+0300")).isEqualTo(expected)
        assertThat(resolve(pattern = pattern, text = "2024-01-15T10:23:45.123Z"))
            .isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
    }

    @Test
    fun `explicit offset wins over the default offset`() {
        val resolved = resolve(
            pattern = "yyyy-MM-ddTHH:mm:ss.SSSXXX",
            text = "2024-01-15T10:23:45.123-05:00",
            utcOffsetMinutes = 120,
        )

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T15:23:45.123Z"))
    }

    @Test
    fun `applies the default offset when the pattern carries none`() {
        val resolved = resolve(pattern = "yyyy-MM-dd HH:mm:ss", text = "2024-01-15 10:23:45", utcOffsetMinutes = 180)

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T07:23:45Z"))
    }

    @Test
    fun `fills the missing year from the reference date`() {
        val resolved = resolve(pattern = "MM-dd HH:mm:ss.SSS", text = "01-15 10:23:45.123")

        assertThat(resolved).isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
    }

    @Test
    fun `fills the missing date from the reference date`() {
        val resolved = resolve(pattern = "HH:mm:ss", text = "10:23:45")

        assertThat(resolved).isEqualTo(Instant.parse("2024-06-05T10:23:45Z"))
    }

    @Test
    fun `resolves epoch based patterns`() {
        assertThat(resolve(pattern = "epochMillis", text = "1705314225123"))
            .isEqualTo(Instant.parse("2024-01-15T10:23:45.123Z"))
        assertThat(resolve(pattern = "epochSeconds", text = "1705314225"))
            .isEqualTo(Instant.parse("2024-01-15T10:23:45Z"))
    }

    @Test
    fun `rolls a time only pattern over midnight`() {
        val resolved = resolve(
            pattern = "HH:mm:ss",
            text = "00:05:00",
            previous = Instant.parse("2024-06-05T23:50:00Z"),
        )

        assertThat(resolved).isEqualTo(Instant.parse("2024-06-06T00:05:00Z"))
    }

    @Test
    fun `does not roll over a small backward jump`() {
        val resolved = resolve(
            pattern = "HH:mm:ss",
            text = "10:25:00",
            previous = Instant.parse("2024-06-05T10:30:00Z"),
        )

        assertThat(resolved).isEqualTo(Instant.parse("2024-06-05T10:25:00Z"))
    }

    @Test
    fun `does not roll over when the pattern carries a date`() {
        val resolved = resolve(
            pattern = "yyyy-MM-dd HH:mm:ss",
            text = "2024-06-05 00:05:00",
            previous = Instant.parse("2024-06-05T23:50:00Z"),
        )

        assertThat(resolved).isEqualTo(Instant.parse("2024-06-05T00:05:00Z"))
    }

    @Test
    fun `fails for a pattern without any known token`() {
        val error = assertFailsWith<IllegalArgumentException> { TimestampPatternCompiler.compile(pattern = "???") }

        assertThat(error.message).contains("no known token")
    }

    @Test
    fun `fails for a blank pattern`() {
        assertFailsWith<IllegalArgumentException> { TimestampPatternCompiler.compile(pattern = "   ") }
    }

    @Test
    fun `fails for a repeated token`() {
        val error = assertFailsWith<IllegalArgumentException> { TimestampPatternCompiler.compile(pattern = "HH:HH") }

        assertThat(error.message).contains("more than once")
    }

    @Test
    fun `returns null for values that are not a valid date`() {
        val resolved = resolve(pattern = "yyyy-MM-dd HH:mm:ss", text = "2024-13-45 10:23:45")

        assertThat(resolved).isNull()
    }

    private fun resolve(
        pattern: String,
        text: String,
        utcOffsetMinutes: Int = 0,
        previous: Instant? = null,
    ): Instant? {
        val compiled = TimestampPatternCompiler.compile(pattern = pattern)
        val match = Regex(pattern = compiled.regexSource).find(input = text)
        assertThat(match).isNotNull()
        return compiled.resolve(
            match = requireNotNull(match),
            context = TimestampResolutionContext(
                referenceDate = REFERENCE_DATE,
                utcOffsetMinutes = utcOffsetMinutes,
                previous = previous,
            ),
        )
    }

    private companion object {
        val REFERENCE_DATE = LocalDate(year = 2024, monthNumber = 6, dayOfMonth = 5)
    }
}
