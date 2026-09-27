package dev.mj31.logger.client.app.usecase.ingest

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveStartDayUseCase
import dev.mj31.logger.client.app.usecase.ingest.date.StartDayResolution
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Which day a log belongs to when its records only say the time.
 *
 * The parser rolls the day forward when time appears to go backwards, so a file running from the
 * evening into the small hours is read as two days. Which two is not written in the file, and the
 * only outside evidence is the moment the file was last written.
 */
class ResolveStartDayUseCaseTest {

    private val useCase = ResolveStartDayUseCase(timeZone = TimeZone.UTC)

    @Test
    fun `a format that carries its own date is never second guessed`() {
        val resolution = useCase(
            source = source(pattern = "yyyy-MM-dd HH:mm:ss", first = at(day = 15, hour = 22), last = at(day = 16, hour = 2)),
            assumedStart = ASSUMED,
            modifiedAt = at(day = 20, hour = 12),
        )

        assertThat(resolution).isEqualTo(StartDayResolution.Settled(day = ASSUMED))
    }

    @Test
    fun `a log that stays inside one day raises no question`() {
        val resolution = useCase(
            source = source(first = at(day = 15, hour = 9), last = at(day = 15, hour = 17)),
            assumedStart = ASSUMED,
            modifiedAt = at(day = 15, hour = 18),
        )

        assertThat(resolution).isEqualTo(StartDayResolution.Settled(day = ASSUMED))
    }

    @Test
    fun `the day before is taken when it is the only one that fits`() {
        // Placed on the assumed day the log would end after the file was last written, which cannot be.
        val resolution = useCase(
            source = source(first = at(day = 15, hour = 22), last = at(day = 16, hour = 2)),
            assumedStart = ASSUMED,
            modifiedAt = at(day = 15, hour = 3),
        )

        assertThat(resolution).isEqualTo(StartDayResolution.Settled(day = LocalDate(2024, 1, 14)))
    }

    @Test
    fun `two days that both fit are put to the user`() {
        val resolution = useCase(
            source = source(first = at(day = 15, hour = 22), last = at(day = 16, hour = 2)),
            assumedStart = ASSUMED,
            modifiedAt = at(day = 16, hour = 3),
        )

        assertThat(resolution).isEqualTo(
            StartDayResolution.Ambiguous(candidates = listOf(ASSUMED, LocalDate(2024, 1, 14))),
        )
    }

    @Test
    fun `without a modification time there is nothing to check the placement against`() {
        val resolution = useCase(
            source = source(first = at(day = 15, hour = 22), last = at(day = 16, hour = 2)),
            assumedStart = ASSUMED,
            modifiedAt = null,
        )

        assertThat(resolution).isEqualTo(StartDayResolution.Settled(day = ASSUMED))
    }

    private fun source(
        pattern: String = "HH:mm:ss",
        first: Instant,
        last: Instant,
    ): LogSource = LogSource(
        id = "src-1",
        name = "app.log",
        path = "/logs/app.log",
        format = LogFormatSpec.Regex(name = "time only", linePattern = "(?<ts>.*)", timestampPattern = pattern),
        entries = listOf(entry(id = "1", timestamp = first), entry(id = "2", timestamp = last)),
        referenceDate = ASSUMED,
    )

    private fun entry(id: String, timestamp: Instant): LogEntry = LogEntry(
        id = id,
        sourceId = "src-1",
        lineNumber = 1,
        timestamp = timestamp,
        level = LogLevel.INFO,
        tag = "Tag",
        message = "message",
        rawLine = "raw",
    )

    private fun at(day: Int, hour: Int): Instant =
        LocalDateTime(year = 2024, monthNumber = 1, dayOfMonth = day, hour = hour, minute = 0)
            .toInstant(timeZone = TimeZone.UTC)

    private companion object {
        val ASSUMED = LocalDate(2024, 1, 15)
    }
}
