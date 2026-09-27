package dev.mj31.logger.client.app.usecase.ingest.duplicate

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlin.test.Test
import kotlinx.datetime.LocalDate

class DetectDuplicateUseCaseTest {

    private val detect = DetectDuplicateUseCase()
    private val open = source(id = "a", path = "/logs/a.log", offsets = listOf(0L, 1_000L, 2_000L))

    @Test
    fun `a path already open is known before the file is read, including a merged part`() {
        val merged = open.copy(extraParts = listOf(LogSourcePart(path = "/logs/a.1.log", name = "a.1.log", referenceDate = null)))

        assertThat(detect(path = "/logs/a.log", sources = listOf(merged))).isEqualTo(merged)
        assertThat(detect(path = "/logs/a.1.log", sources = listOf(merged))).isEqualTo(merged)
        assertThat(detect(path = "/logs/b.log", sources = listOf(merged))).isNull()
    }

    @Test
    fun `the same records under another path are a copy`() {
        val copy = source(id = "b", path = "/backup/a.log", offsets = listOf(0L, 1_000L, 2_000L))

        assertThat(detect(candidate = copy, sources = listOf(open))).isEqualTo(DuplicateVerdict.SameContent(existing = open))
    }

    @Test
    fun `some records in common under the same format and zone are an overlap`() {
        val rotated = source(id = "b", path = "/logs/a.1.log", offsets = listOf(2_000L, 3_000L))

        assertThat(detect(candidate = rotated, sources = listOf(open)))
            .isEqualTo(DuplicateVerdict.Overlap(existing = open, sharedRecordCount = 1))
    }

    @Test
    fun `records in common read in another zone are not one log`() {
        val elsewhere = source(id = "b", path = "/logs/a.1.log", offsets = listOf(2_000L, 3_000L))
            .copy(zone = SourceZone(id = "Asia/Tokyo", origin = ZoneOrigin.CHOSEN))

        assertThat(detect(candidate = elsewhere, sources = listOf(open))).isEqualTo(DuplicateVerdict.Distinct)
    }

    @Test
    fun `a file with nothing in common is distinct`() {
        val other = source(id = "b", path = "/logs/b.log", offsets = listOf(9_000L))

        assertThat(detect(candidate = other, sources = listOf(open))).isEqualTo(DuplicateVerdict.Distinct)
    }

    @Test
    fun `a file that holds the whole open one and more is an overlap, not a copy`() {
        val longer = source(id = "b", path = "/logs/a.later.log", offsets = listOf(0L, 1_000L, 2_000L, 3_000L))

        assertThat(detect(candidate = longer, sources = listOf(open)))
            .isEqualTo(DuplicateVerdict.Overlap(existing = open, sharedRecordCount = 3))
    }

    private fun source(id: String, path: String, offsets: List<Long>): LogSource = LogSource(
        id = id,
        name = path.substringAfterLast(delimiter = '/'),
        path = path,
        format = TestLogEntries.SPEC,
        entries = offsets.mapIndexed { index, offset ->
            LogEntry(
                id = "$id:${index + 1}",
                sourceId = id,
                lineNumber = index + 1,
                timestamp = TestLogEntries.at(offsetMillis = offset),
                level = TestLogEntries.SPEC.fallbackLevel,
                tag = "App",
                message = "record $offset",
                rawLine = "$offset record",
            )
        },
        referenceDate = LocalDate(year = 2024, monthNumber = 5, dayOfMonth = 1),
    )
}
