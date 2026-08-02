package dev.mj31.logger.client.app.usecase.session

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.test.Test

class FilterLogEntriesUseCaseTest {

    private val filterEntries = FilterLogEntriesUseCase()

    private val entries = listOf(
        TestLogEntries.entry(id = "1", level = LogLevel.INFO, message = "Connected", sourceId = "src-a"),
        TestLogEntries.entry(id = "2", level = LogLevel.ERROR, message = "Timeout", sourceId = "src-a"),
        TestLogEntries.entry(id = "3", level = LogLevel.ERROR, message = "Retry", sourceId = "src-b"),
    )

    @Test
    fun `an inactive filter returns the very same list instance`() {
        val result = filterEntries(entries = entries, filter = LogFilter.NONE)

        assertThat(result).isSameInstanceAs(entries)
    }

    @Test
    fun `a blank query does not filter anything out`() {
        val result = filterEntries(entries = entries, filter = LogFilter(query = "  "))

        assertThat(result).isSameInstanceAs(entries)
    }

    @Test
    fun `an active filter keeps only the matching entries`() {
        val result = filterEntries(entries = entries, filter = LogFilter(levels = setOf(LogLevel.ERROR)))

        assertThat(result.map { it.id }).containsExactly("2", "3").inOrder()
    }

    @Test
    fun `combined criteria narrow the result further`() {
        val filter = LogFilter(levels = setOf(LogLevel.ERROR), sourceIds = setOf("src-b"))

        val result = filterEntries(entries = entries, filter = filter)

        assertThat(result.map { it.id }).containsExactly("3")
    }

    @Test
    fun `a filter matching nothing yields an empty list`() {
        val result = filterEntries(entries = entries, filter = LogFilter(query = "nothing matches this"))

        assertThat(result).isEmpty()
    }

    @Test
    fun `filtering an empty list stays empty`() {
        val result = filterEntries(entries = emptyList(), filter = LogFilter(levels = setOf(LogLevel.WARN)))

        assertThat(result).isEmpty()
    }
}
