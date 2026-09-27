package dev.mj31.logger.client.app.usecase.ingest.duplicate

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import kotlin.test.Test
import kotlinx.datetime.LocalDate

class MergeSourcePartsUseCaseTest {

    private val merge = MergeSourcePartsUseCase()

    @Test
    fun `shared records are kept once and the rest of both files follows in time order`() {
        val base = source(id = "a", path = "/a.log", records = listOf(0L to "boot", 1_000L to "ready", 2_000L to "tick"))
        val addition = source(id = "b", path = "/a.1.log", records = listOf(2_000L to "tick", 3_000L to "done"))

        val merged = merge(base = base, addition = addition)

        assertThat(merged.entries.map { it.message }).containsExactly("boot", "ready", "tick", "done").inOrder()
        assertThat(merged.paths).containsExactly("/a.log", "/a.1.log").inOrder()
    }

    @Test
    fun `the base keeps its records as they were and the addition's take its identity`() {
        val base = source(id = "a", path = "/a.log", records = listOf(0L to "boot"))
        val addition = source(id = "b", path = "/a.1.log", records = listOf(3_000L to "done"))

        val merged = merge(base = base, addition = addition)

        assertThat(merged.id).isEqualTo("a")
        assertThat(merged.entries.first()).isEqualTo(base.entries.single())
        assertThat(merged.entries.last().sourceId).isEqualTo("a")
        assertThat(merged.entries.last().id).isEqualTo("a:1:1")
    }

    @Test
    fun `a line written twice in the same moment is two records, not one`() {
        val base = source(id = "a", path = "/a.log", records = listOf(0L to "retry"))
        val addition = source(id = "b", path = "/a.1.log", records = listOf(0L to "retry", 0L to "retry"))

        val merged = merge(base = base, addition = addition)

        assertThat(merged.entries.map { it.message }).containsExactly("retry", "retry")
    }

    @Test
    fun `a record with the same line but another continuation is a different record`() {
        val base = source(id = "a", path = "/a.log", records = listOf(0L to "Boom\n\tat A.kt:1"))
        val addition = source(id = "b", path = "/a.1.log", records = listOf(0L to "Boom\n\tat B.kt:2"))

        assertThat(merge(base = base, addition = addition).entryCount).isEqualTo(2)
    }

    @Test
    fun `a third file is a second part and numbers its records apart`() {
        val base = source(id = "a", path = "/a.log", records = listOf(0L to "boot"))
        val twice = merge(
            base = merge(base = base, addition = source(id = "b", path = "/a.1.log", records = listOf(1_000L to "one"))),
            addition = source(id = "c", path = "/a.2.log", records = listOf(2_000L to "two")),
        )

        assertThat(twice.extraParts.map { it.path }).containsExactly("/a.1.log", "/a.2.log").inOrder()
        assertThat(twice.extraParts.last()).isEqualTo(
            LogSourcePart(path = "/a.2.log", name = "a.2.log", referenceDate = DAY),
        )
        assertThat(twice.entries.last().id).isEqualTo("a:2:1")
    }

    private fun source(id: String, path: String, records: List<Pair<Long, String>>): LogSource = LogSource(
        id = id,
        name = path.removePrefix(prefix = "/"),
        path = path,
        format = TestLogEntries.SPEC,
        entries = records.mapIndexed { index, (offset, message) ->
            val line = message.substringBefore(delimiter = '\n')
            LogEntry(
                id = "$id:${index + 1}",
                sourceId = id,
                lineNumber = index + 1,
                timestamp = TestLogEntries.at(offsetMillis = offset),
                level = TestLogEntries.SPEC.fallbackLevel,
                tag = "App",
                message = message,
                rawLine = "$offset $line",
            )
        },
        referenceDate = DAY,
    )

    private companion object {
        val DAY = LocalDate(year = 2024, monthNumber = 5, dayOfMonth = 1)
    }
}
