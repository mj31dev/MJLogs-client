package dev.mj31.logger.client.data.repository

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Storage keeps the imported files apart and in the order they arrived; merging them into one
 * chronological session is the job of the use case above it.
 */
class InMemoryLogSessionRepositoryTest {

    private val repository = InMemoryLogSessionRepository()

    @Test
    fun `starts empty`() = runTest {
        assertThat(repository.sources.value).isEmpty()
    }

    @Test
    fun `sources are kept in insertion order`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L, 20_000L)))
        repository.addSource(source = source(id = "b", offsets = listOf(10_000L)))

        val sources = repository.sources.value
        assertThat(sources.map { it.id }).containsExactly("a", "b").inOrder()
        assertThat(sources.flatMap { it.entries }.map { it.id })
            .containsExactly("a:1", "a:2", "b:1")
            .inOrder()
    }

    @Test
    fun `adding a source twice replaces it instead of duplicating`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L, 1_000L)))
        repository.addSource(source = source(id = "a", offsets = listOf(5_000L)))

        assertThat(repository.sources.value).hasSize(1)
        assertThat(repository.sources.value.single().entries.map { it.id }).containsExactly("a:1")
    }

    @Test
    fun `replacing a known source keeps its position`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L)))
        repository.addSource(source = source(id = "b", offsets = listOf(1_000L)))

        repository.replaceSource(source = source(id = "a", offsets = listOf(2_000L, 3_000L)))

        assertThat(repository.sources.value.map { it.id }).containsExactly("a", "b").inOrder()
        assertThat(repository.sources.value.flatMap { it.entries }).hasSize(3)
    }

    @Test
    fun `replacing an unknown source appends it`() = runTest {
        repository.replaceSource(source = source(id = "a", offsets = listOf(0L)))

        assertThat(repository.sources.value.map { it.id }).containsExactly("a")
    }

    @Test
    fun `removing a source drops its entries only`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L)))
        repository.addSource(source = source(id = "b", offsets = listOf(1_000L)))

        repository.removeSource(sourceId = "a")

        assertThat(repository.sources.value.map { it.id }).containsExactly("b")
        assertThat(repository.sources.value.flatMap { it.entries }.map { it.sourceId })
            .containsExactly("b")
    }

    @Test
    fun `removing an unknown source changes nothing`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L)))

        repository.removeSource(sourceId = "missing")

        assertThat(repository.sources.value).hasSize(1)
    }

    @Test
    fun `clearing empties the storage`() = runTest {
        repository.addSource(source = source(id = "a", offsets = listOf(0L)))

        repository.clear()

        assertThat(repository.sources.value).isEmpty()
    }

    @Test
    fun `every change publishes a new immutable list`() = runTest {
        val first = repository.sources.value
        repository.addSource(source = source(id = "a", offsets = listOf(0L)))
        val second = repository.sources.value

        assertThat(second).isNotSameInstanceAs(first)
        assertThat(first).isEmpty()
    }

    private fun source(id: String, offsets: List<Long>): LogSource = LogSource(
        id = id,
        name = "$id.txt",
        path = "/logs/$id.txt",
        format = SPEC,
        entries = offsets.mapIndexed { index, offset ->
            LogEntry(
                id = "$id:${index + 1}",
                sourceId = id,
                lineNumber = index + 1,
                timestamp = BASE + offset.milliseconds,
                level = LogLevel.INFO,
                tag = "Tag",
                message = "message $index",
                rawLine = "raw",
            )
        },
    )

    private companion object {
        val BASE: Instant = Instant.parse("2024-05-01T10:00:00Z")
        val SPEC = LogFormatSpec(
            name = "test",
            linePattern = "(?<ts>.*)",
            timestampPattern = "epochMillis",
            origin = FormatOrigin.DETECTED,
        )
    }
}
