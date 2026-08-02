package dev.mj31.logger.client.domain.model

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.fake.TestLogEntries
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.time.TimeRange

class TimeRangeTest {

    private val range = TimeRange(
        start = TestLogEntries.at(offsetMillis = 1_000L),
        end = TestLogEntries.at(offsetMillis = 4_000L),
    )

    @Test
    fun `duration is the distance between both bounds`() {
        assertThat(range.durationMillis).isEqualTo(3_000L)
    }

    @Test
    fun `duration of a collapsed range is zero`() {
        val collapsed = TimeRange(start = TestLogEntries.BASE, end = TestLogEntries.BASE)

        assertThat(collapsed.durationMillis).isEqualTo(0L)
    }

    @Test
    fun `contains is inclusive on both bounds`() {
        assertThat(TestLogEntries.at(offsetMillis = 1_000L) in range).isTrue()
        assertThat(TestLogEntries.at(offsetMillis = 4_000L) in range).isTrue()
        assertThat(TestLogEntries.at(offsetMillis = 2_500L) in range).isTrue()
    }

    @Test
    fun `contains rejects instants outside the interval`() {
        assertThat(TestLogEntries.at(offsetMillis = 999L) in range).isFalse()
        assertThat(TestLogEntries.at(offsetMillis = 4_001L) in range).isFalse()
    }

    @Test
    fun `of returns null for an empty list of instants`() {
        assertThat(TimeRange.of(instants = emptyList())).isNull()
    }

    @Test
    fun `of derives the bounds from unordered instants`() {
        val derived = TimeRange.of(
            instants = listOf(
                TestLogEntries.at(offsetMillis = 4_000L),
                TestLogEntries.at(offsetMillis = 1_000L),
                TestLogEntries.at(offsetMillis = 2_000L),
            ),
        )

        assertThat(derived).isEqualTo(range)
    }

    @Test
    fun `of a single instant collapses the range`() {
        val derived = TimeRange.of(instants = listOf(TestLogEntries.BASE))

        assertThat(derived).isEqualTo(TimeRange(start = TestLogEntries.BASE, end = TestLogEntries.BASE))
    }
}
