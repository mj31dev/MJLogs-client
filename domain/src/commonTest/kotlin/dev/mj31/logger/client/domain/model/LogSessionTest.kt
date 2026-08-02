package dev.mj31.logger.client.domain.model

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.fake.TestLogEntries
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.model.log.LogSession

class LogSessionTest {

    private val firstSource = TestLogEntries.source(
        id = "src-a",
        entries = listOf(
            TestLogEntries.entryAt(offsetMillis = 0L, id = "src-a:1", sourceId = "src-a"),
            TestLogEntries.entryAt(offsetMillis = 2_000L, id = "src-a:2", sourceId = "src-a"),
        ),
    )
    private val secondSource = TestLogEntries.source(
        id = "src-b",
        name = "net.txt",
        path = "/logs/net.txt",
        entries = listOf(TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-b:1", sourceId = "src-b")),
    )

    @Test
    fun `empty session has no entries and no time range`() {
        assertThat(LogSession.EMPTY.isEmpty).isTrue()
        assertThat(LogSession.EMPTY.timeRange).isNull()
        assertThat(LogSession.EMPTY.entries).isEmpty()
        assertThat(LogSession.EMPTY.sources).isEmpty()
    }

    @Test
    fun `session time range spans the first and the last entry`() {
        val session = LogSession(
            sources = listOf(firstSource, secondSource),
            entries = listOf(
                firstSource.entries[0],
                secondSource.entries[0],
                firstSource.entries[1],
            ),
        )

        assertThat(session.isEmpty).isFalse()
        assertThat(session.timeRange).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 0L),
                end = TestLogEntries.at(offsetMillis = 2_000L),
            ),
        )
    }

    @Test
    fun `source lookup returns the matching source or null`() {
        val session = LogSession(sources = listOf(firstSource, secondSource), entries = emptyList())

        assertThat(session.sourceById(sourceId = "src-b")).isEqualTo(secondSource)
        assertThat(session.sourceById(sourceId = "missing")).isNull()
    }

    @Test
    fun `source without entries has no time range`() {
        val empty = TestLogEntries.source(entries = emptyList())

        assertThat(empty.entryCount).isEqualTo(0)
        assertThat(empty.timeRange).isNull()
    }

    @Test
    fun `source time range is derived from its own entries`() {
        assertThat(firstSource.entryCount).isEqualTo(2)
        assertThat(firstSource.timeRange).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 0L),
                end = TestLogEntries.at(offsetMillis = 2_000L),
            ),
        )
    }

    @Test
    fun `source keeps the count of lines skipped before the first record`() {
        val source = TestLogEntries.source(entries = firstSource.entries, skippedLineCount = 3)

        assertThat(source.skippedLineCount).isEqualTo(3)
    }
}
