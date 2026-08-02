package dev.mj31.logger.client.app.usecase.session

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import kotlin.test.Test

class MergeLogSourcesUseCaseTest {

    private val merge = MergeLogSourcesUseCase()

    @Test
    fun `entries of several sources are interleaved chronologically`() {
        val first = TestLogEntries.source(
            id = "src-a",
            entries = listOf(
                TestLogEntries.entryAt(offsetMillis = 0L, id = "src-a:1", sourceId = "src-a"),
                TestLogEntries.entryAt(offsetMillis = 4_000L, id = "src-a:2", sourceId = "src-a"),
            ),
        )
        val second = TestLogEntries.source(
            id = "src-b",
            entries = listOf(
                TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-b:1", sourceId = "src-b"),
                TestLogEntries.entryAt(offsetMillis = 6_000L, id = "src-b:2", sourceId = "src-b"),
            ),
        )

        val session = merge(sources = listOf(first, second))

        assertThat(session.entries.map { it.id })
            .containsExactly("src-a:1", "src-b:1", "src-a:2", "src-b:2")
            .inOrder()
        assertThat(session.sources).containsExactly(first, second).inOrder()
        assertThat(session.isEmpty).isFalse()
    }

    @Test
    fun `equal timestamps keep the import order of the sources`() {
        val first = TestLogEntries.source(
            id = "src-a",
            entries = listOf(
                TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-a:1", sourceId = "src-a"),
                TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-a:2", sourceId = "src-a"),
            ),
        )
        val second = TestLogEntries.source(
            id = "src-b",
            entries = listOf(
                TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-b:1", sourceId = "src-b"),
                TestLogEntries.entryAt(offsetMillis = 1_000L, id = "src-b:2", sourceId = "src-b"),
            ),
        )

        val session = merge(sources = listOf(first, second))

        assertThat(session.entries.map { it.id })
            .containsExactly("src-a:1", "src-a:2", "src-b:1", "src-b:2")
            .inOrder()
    }

    @Test
    fun `merged session exposes the overall time range`() {
        val source = TestLogEntries.source(
            entries = listOf(
                TestLogEntries.entryAt(offsetMillis = 5_000L, id = "src-1:2"),
                TestLogEntries.entryAt(offsetMillis = 500L, id = "src-1:1"),
            ),
        )

        val session = merge(sources = listOf(source))

        assertThat(session.timeRange?.start).isEqualTo(TestLogEntries.at(offsetMillis = 500L))
        assertThat(session.timeRange?.end).isEqualTo(TestLogEntries.at(offsetMillis = 5_000L))
    }

    @Test
    fun `merging no source yields an empty session`() {
        val session = merge(sources = emptyList())

        assertThat(session.isEmpty).isTrue()
        assertThat(session.entries).isEmpty()
        assertThat(session.sources).isEmpty()
        assertThat(session.timeRange).isNull()
    }

    @Test
    fun `sources without entries still belong to the session`() {
        val empty = TestLogEntries.source(id = "src-empty", entries = emptyList())

        val session = merge(sources = listOf(empty))

        assertThat(session.sources).containsExactly(empty)
        assertThat(session.entries).isEmpty()
    }
}
