package dev.mj31.logger.client.app.usecase.ingest.zone

import dev.mj31.logger.client.app.usecase.ingest.source.RebuildSourceUseCase
import dev.mj31.logger.client.app.usecase.ingest.duplicate.MergeSourcePartsUseCase
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParser
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParserFactory
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.app.fake.source.FakeLogFileExpander
import dev.mj31.logger.client.app.fake.source.FakeTextFileDataSource
import dev.mj31.logger.client.app.fake.source.FixedIdGenerator
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.data.repository.InMemoryLogSessionRepository
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import dev.mj31.logger.client.domain.source.TextFileContent
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeSourceZoneUseCaseTest {

    private val factory = ScriptedLogLineParserFactory()
    private val sessionRepository = InMemoryLogSessionRepository()
    private val dataSource = FakeTextFileDataSource.of(
        content = TextFileContent(
            path = PATH,
            name = "app.log",
            lines = listOf(
                "TZ: Europe/Berlin",
                ScriptedLogLineParser.recordLine(timestamp = TestLogEntries.at(offsetMillis = 0L)),
            ),
        ),
    )
    private val changeZone = ChangeSourceZoneUseCase(
        rebuildSource = RebuildSourceUseCase(
            loader = LogSourceLoader(
                dataSource = dataSource,
                assembler = LogSourceAssembler(parserFactory = factory),
                idGenerator = FixedIdGenerator(ids = listOf("unused")),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            mergeParts = MergeSourcePartsUseCase(),
        ),
        sessionRepository = sessionRepository,
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `the file is read again in the chosen zone and keeps its identity and day`() = runTest {
        sessionRepository.addSource(source = loaded())

        val changed = changeZone(sourceId = "src-1", zoneId = "Asia/Tokyo")

        val stored = sessionRepository.sources.value.single()
        assertThat(changed).isEqualTo(stored)
        assertThat(stored.id).isEqualTo("src-1")
        assertThat(stored.referenceDate).isEqualTo(DAY)
        assertThat(stored.zone).isEqualTo(SourceZone(id = "Asia/Tokyo", origin = ZoneOrigin.CHOSEN))
        assertThat(stored.format.zoneId).isEqualTo("Asia/Tokyo")
        assertThat(factory.createdSpecs.last().zoneId).isEqualTo("Asia/Tokyo")
    }

    @Test
    fun `handing the choice back lets the file name its zone again`() = runTest {
        sessionRepository.addSource(source = loaded(zoneId = "Asia/Tokyo"))

        changeZone(sourceId = "src-1", zoneId = null)

        val stored = sessionRepository.sources.value.single()
        assertThat(stored.zone).isEqualTo(SourceZone(id = "Europe/Berlin", origin = ZoneOrigin.HEADER))
        assertThat(stored.format.zoneId).isNull()
    }

    @Test
    fun `a file that can no longer be read leaves the session as it was`() = runTest {
        val before = loaded()
        sessionRepository.addSource(source = before)
        dataSource.registerFailure(path = PATH, error = IllegalStateException("gone"))

        val changed = changeZone(sourceId = "src-1", zoneId = "Asia/Tokyo")

        assertThat(changed).isNull()
        assertThat(sessionRepository.sources.value).containsExactly(before)
    }

    @Test
    fun `an unknown source changes nothing`() = runTest {
        assertThat(changeZone(sourceId = "missing", zoneId = "Asia/Tokyo")).isNull()
    }

    private fun loaded(zoneId: String? = null): LogSource = LogSource(
        id = "src-1",
        name = "app.log",
        path = PATH,
        format = TestLogEntries.SPEC.withZoneId(zoneId = zoneId),
        entries = emptyList(),
        referenceDate = DAY,
    )

    private companion object {
        const val PATH = "/logs/app.log"
        val DAY = LocalDate(year = 2024, monthNumber = 5, dayOfMonth = 1)
    }
}
