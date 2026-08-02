package dev.mj31.logger.client.domain.model

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.fake.TestLogEntries
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogFilter

class LogFilterTest {

    private val entry = TestLogEntries.entry(
        sourceId = "src-a",
        timestamp = TestLogEntries.at(offsetMillis = 5_000L),
        level = LogLevel.WARN,
        tag = "Network",
        message = "Connection lost",
        rawLine = "10:00:05 W Network: Connection lost",
    )

    @Test
    fun `default filter is inactive`() {
        assertThat(LogFilter().isActive).isFalse()
        assertThat(LogFilter.NONE.isActive).isFalse()
    }

    @Test
    fun `blank query does not activate the filter`() {
        assertThat(LogFilter(query = "   ").isActive).isFalse()
    }

    @Test
    fun `any single criterion activates the filter`() {
        assertThat(LogFilter(query = "boom").isActive).isTrue()
        assertThat(LogFilter(levels = setOf(LogLevel.ERROR)).isActive).isTrue()
        assertThat(LogFilter(sourceIds = setOf("src-a")).isActive).isTrue()
        val range = TimeRange(start = TestLogEntries.BASE, end = TestLogEntries.at(offsetMillis = 1_000L))
        assertThat(LogFilter(timeRange = range).isActive).isTrue()
    }

    @Test
    fun `inactive filter matches every entry`() {
        assertThat(LogFilter.NONE.matches(entry = entry)).isTrue()
    }

    @Test
    fun `empty level set means no level restriction`() {
        val filter = LogFilter(levels = emptySet(), sourceIds = setOf("src-a"))

        assertThat(filter.matches(entry = entry)).isTrue()
    }

    @Test
    fun `empty source set means no source restriction`() {
        val filter = LogFilter(levels = setOf(LogLevel.WARN), sourceIds = emptySet())

        assertThat(filter.matches(entry = entry)).isTrue()
    }

    @Test
    fun `level criterion keeps only the selected levels`() {
        val filter = LogFilter(levels = setOf(LogLevel.ERROR, LogLevel.FATAL))

        assertThat(filter.matches(entry = entry)).isFalse()
        assertThat(filter.matches(entry = entry.copy(level = LogLevel.ERROR))).isTrue()
    }

    @Test
    fun `source criterion keeps only the selected sources`() {
        val filter = LogFilter(sourceIds = setOf("src-b"))

        assertThat(filter.matches(entry = entry)).isFalse()
        assertThat(filter.matches(entry = entry.copy(sourceId = "src-b"))).isTrue()
    }

    @Test
    fun `time range criterion is inclusive on both bounds`() {
        val filter = LogFilter(
            timeRange = TimeRange(
                start = TestLogEntries.at(offsetMillis = 5_000L),
                end = TestLogEntries.at(offsetMillis = 9_000L),
            ),
        )

        assertThat(filter.matches(entry = entry)).isTrue()
        assertThat(filter.matches(entry = entry.copy(timestamp = TestLogEntries.at(offsetMillis = 9_000L)))).isTrue()
        assertThat(filter.matches(entry = entry.copy(timestamp = TestLogEntries.at(offsetMillis = 4_999L)))).isFalse()
        assertThat(filter.matches(entry = entry.copy(timestamp = TestLogEntries.at(offsetMillis = 9_001L)))).isFalse()
    }

    @Test
    fun `query matches the tag ignoring case`() {
        val filter = LogFilter(query = "netWORK")

        assertThat(filter.matches(entry = entry.copy(message = "", rawLine = ""))).isTrue()
    }

    @Test
    fun `query matches the message ignoring case`() {
        val filter = LogFilter(query = "CONNECTION lost")

        assertThat(filter.matches(entry = entry.copy(tag = "", rawLine = ""))).isTrue()
    }

    @Test
    fun `query matches the raw line even when tag and message do not contain it`() {
        val filter = LogFilter(query = "10:00:05")

        assertThat(filter.matches(entry = entry)).isTrue()
        assertThat(filter.matches(entry = entry.copy(rawLine = "no timestamp here"))).isFalse()
    }

    @Test
    fun `query is trimmed before matching`() {
        val filter = LogFilter(query = "  Connection  ")

        assertThat(filter.matches(entry = entry)).isTrue()
    }

    @Test
    fun `unmatched query rejects the entry`() {
        assertThat(LogFilter(query = "database").matches(entry = entry)).isFalse()
    }

    @Test
    fun `combined criteria are joined with a logical and`() {
        val filter = LogFilter(
            query = "connection",
            levels = setOf(LogLevel.WARN),
            sourceIds = setOf("src-a"),
            timeRange = TimeRange(
                start = TestLogEntries.BASE,
                end = TestLogEntries.at(offsetMillis = 10_000L),
            ),
        )

        assertThat(filter.matches(entry = entry)).isTrue()
        assertThat(filter.matches(entry = entry.copy(level = LogLevel.INFO))).isFalse()
        assertThat(filter.matches(entry = entry.copy(sourceId = "src-b"))).isFalse()
        assertThat(filter.matches(entry = entry.copy(message = "all good", rawLine = "all good", tag = "UI"))).isFalse()
        assertThat(filter.matches(entry = entry.copy(timestamp = TestLogEntries.at(offsetMillis = 20_000L)))).isFalse()
    }

    @Test
    fun `entry text matching covers tag message and raw line`() {
        assertThat(entry.matchesText(query = "netw")).isTrue()
        assertThat(entry.matchesText(query = "lost")).isTrue()
        assertThat(entry.matchesText(query = "10:00")).isTrue()
        assertThat(entry.matchesText(query = "absent")).isFalse()
    }
}
