package dev.mj31.logger.client.app

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.FakeVideoPlayer
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerRepositories
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerUseCases
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.data.repository.InMemoryLogSessionRepository
import dev.mj31.logger.client.data.repository.InMemorySyncRepository
import dev.mj31.logger.client.data.repository.InMemoryVideoRepository
import dev.mj31.logger.client.data.source.LocalTextFileDataSource
import dev.mj31.logger.client.data.source.UuidIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.test.Test
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapLogTimeToVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.fake.video.fakeAutoSynchronize
import dev.mj31.logger.client.app.usecase.playback.StepVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ClearSynchronizationUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.usecase.session.FilterLogEntriesUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.preview.RegexLogFormatPreviewer
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory
import dev.mj31.logger.client.data.format.detect.HeuristicLogFormatDetector
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerStateAssembler
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeAtTimestampUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ComposeFrameTimeUseCase

/**
 * Full vertical slice: real files, real format detection, real parsing and merging, real use cases,
 * with only the video engine replaced by a scriptable double.
 *
 * It is the automated equivalent of loading the shipped samples in the running application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndSessionTest {

    private val samples = File("../samples")

    @Test
    fun `the shipped samples merge, filter and follow the playhead`() = runTest {
        val player = FakeVideoPlayer()
        val store = buildStore(player = player, testScope = this)

        store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(
                paths = SAMPLE_FILES.map { name -> File(samples, name).absolutePath },
            ),
        )
        testScheduler.advanceUntilIdle()

        val loaded = store.state.value
        assertThat(loaded.sources).hasSize(SAMPLE_FILES.size)
        assertThat(loaded.totalEntryCount).isGreaterThan(MIN_EXPECTED_RECORDS)
        assertThat(loaded.entries.map { it.timestamp }).isInOrder()
        assertThat(loaded.formatRequest).isNull()

        // Filtering keeps the session intact.
        store.handleIntent(
            intent = LogPlayerIntent.UpdateFilter(
                filter = loaded.filter.copy(levels = setOf(LogLevel.ERROR, LogLevel.FATAL)),
            ),
        )
        testScheduler.advanceUntilIdle()
        val filtered = store.state.value
        assertThat(filtered.entries).isNotEmpty()
        assertThat(filtered.entries.all { it.level == LogLevel.ERROR || it.level == LogLevel.FATAL }).isTrue()
        assertThat(filtered.totalEntryCount).isEqualTo(loaded.totalEntryCount)

        store.handleIntent(intent = LogPlayerIntent.UpdateFilter(filter = loaded.filter))
        testScheduler.advanceUntilIdle()

        // Load a screencast and pin the middle record to 00:30.
        store.handleIntent(intent = LogPlayerIntent.ImportVideo(path = File(samples, CLIP_NAME).absolutePath))
        player.setDuration(durationMillis = VIDEO_DURATION_MILLIS)
        player.setPosition(positionMillis = ANCHOR_POSITION_MILLIS)
        testScheduler.advanceUntilIdle()

        val entries = store.state.value.entries
        val anchorEntry = entries[entries.size / 2]
        store.handleIntent(intent = LogPlayerIntent.SelectEntry(entryId = anchorEntry.id))
        store.handleIntent(intent = LogPlayerIntent.Synchronize)
        testScheduler.advanceUntilIdle()

        assertThat(store.state.value.sync.isSynced).isTrue()
        assertThat(store.state.value.activeEntryId).isEqualTo(anchorEntry.id)

        // Moving the playhead forward moves the highlighted record forward as well.
        player.setPosition(positionMillis = ANCHOR_POSITION_MILLIS + 10_000L)
        testScheduler.advanceUntilIdle()
        val later = store.state.value.activeEntryId
        assertThat(later).isNotEqualTo(anchorEntry.id)

        val laterEntry = entries.first { it.id == later }
        assertThat(laterEntry.timestamp).isGreaterThan(anchorEntry.timestamp)

        // Selecting a record seeks the video to the matching position.
        store.handleIntent(intent = LogPlayerIntent.SelectEntry(entryId = anchorEntry.id))
        testScheduler.advanceUntilIdle()
        assertThat(player.seekPositions.last()).isEqualTo(ANCHOR_POSITION_MILLIS)
    }

    @Test
    fun `a file the detector cannot recognize is completed by the user`() = runTest {
        val store = buildStore(player = FakeVideoPlayer(), testScope = this)

        store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(paths = listOf(File(samples, EXOTIC_NAME).absolutePath)),
        )
        testScheduler.advanceUntilIdle()

        assertThat(store.state.value.formatRequest?.fileName).isEqualTo(EXOTIC_NAME)

        // The dialog opens on the inferred layout, so applying it unchanged is the whole gesture.
        val suggested = requireNotNull(store.state.value.formatRequest)
        assertThat(suggested.timestampPattern).isEqualTo("dd.MM.yyyy_HH.mm.ss")
        assertThat(suggested.structureTemplate).isEqualTo("<{any}>~{timestamp}~{tag}~{message}")
        assertThat(suggested.canApply).isTrue()

        store.handleIntent(intent = LogPlayerIntent.SubmitManualFormat)
        testScheduler.advanceUntilIdle()

        val state = store.state.value
        assertThat(state.formatRequest).isNull()
        assertThat(state.sources.single().name).isEqualTo(EXOTIC_NAME)
        assertThat(state.totalEntryCount).isGreaterThan(0)
    }

    @Test
    fun `a file of the wrong type is refused by the real pipeline`() = runTest {
        val store = buildStore(player = FakeVideoPlayer(), testScope = this)

        store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(paths = listOf(File(samples, CLIP_NAME).absolutePath)),
        )
        testScheduler.advanceUntilIdle()

        assertThat(store.state.value.sources).isEmpty()
        assertThat(store.state.value.formatRequest).isNull()
    }

    @Test
    fun `filtering by source narrows the merged session to one file`() = runTest {
        val store = buildStore(player = FakeVideoPlayer(), testScope = this)
        store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(
                paths = SAMPLE_FILES.map { name -> File(samples, name).absolutePath },
            ),
        )
        testScheduler.advanceUntilIdle()

        val network = store.state.value.sources.single { it.name == "network.txt" }
        store.handleIntent(
            intent = LogPlayerIntent.UpdateFilter(
                filter = store.state.value.filter.copy(sourceIds = setOf(network.id)),
            ),
        )
        testScheduler.advanceUntilIdle()

        val entries = store.state.value.entries
        assertThat(entries).isNotEmpty()
        assertThat(entries.map { it.sourceId }.distinct()).containsExactly(network.id)
        assertThat(entries.size).isEqualTo(network.entryCount)
    }

    @Test
    fun `a free text query survives the whole pipeline`() = runTest {
        val store = buildStore(player = FakeVideoPlayer(), testScope = this)
        store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(paths = listOf(File(samples, "network.txt").absolutePath)),
        )
        testScheduler.advanceUntilIdle()

        store.handleIntent(
            intent = LogPlayerIntent.UpdateFilter(filter = store.state.value.filter.copy(query = "SocketTimeout")),
        )
        testScheduler.advanceUntilIdle()

        val entries = store.state.value.entries
        assertThat(entries).isNotEmpty()
        assertThat(entries.all { it.matchesText(query = "SocketTimeout") }).isTrue()
    }

    private fun buildStore(player: FakeVideoPlayer, testScope: TestScope): LogPlayerStore {
        val dispatcher = UnconfinedTestDispatcher(scheduler = testScope.testScheduler)
        val loader = LogSourceLoader(
            dataSource = LocalTextFileDataSource(dispatcher = dispatcher),
            assembler = LogSourceAssembler(parserFactory = RegexLogLineParserFactory()),
            idGenerator = UuidIdGenerator(),
            clock = Clock.System,
            timeZone = TimeZone.UTC,
        )
        val syncRepository = InMemorySyncRepository()
        val parseFrameTime = ParseFrameTimeUseCase()
        return LogPlayerStore(
            repositories = LogPlayerRepositories(
                session = InMemoryLogSessionRepository(),
                video = InMemoryVideoRepository(),
                sync = syncRepository,
            ),
            useCases = LogPlayerUseCases(
                mergeLogSources = MergeLogSourcesUseCase(),
                importLogFile = ImportLogFileUseCase(
                    loader = loader,
                    detector = HeuristicLogFormatDetector(),
                    dispatcher = dispatcher,
                ),
                importLogFileWithFormat = ImportLogFileWithFormatUseCase(loader = loader, dispatcher = dispatcher),
                filterLogEntries = FilterLogEntriesUseCase(),
                synchronizeTimelines = SynchronizeTimelinesUseCase(syncRepository = syncRepository),
                synchronizeAtTimestamp = SynchronizeAtTimestampUseCase(syncRepository = syncRepository),
                parseFrameTime = parseFrameTime,
                composeFrameTime = ComposeFrameTimeUseCase(parseFrameTime = parseFrameTime),
                autoSynchronize = fakeAutoSynchronize(syncRepository = syncRepository),
                stepVideoPosition = StepVideoPositionUseCase(),
                clearSynchronization = ClearSynchronizationUseCase(syncRepository = syncRepository),
                mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
                mapLogTimeToVideoPosition = MapLogTimeToVideoPositionUseCase(),
            ),
            player = player,
            formatTools = LogPlayerFormatTools(
                compiler = TemplateLogFormatCompiler(),
                previewer = RegexLogFormatPreviewer(),
            ),
            stateAssembler = LogPlayerStateAssembler(
                parseFrameTime = parseFrameTime,
                findEntryAtVideoPosition = FindEntryAtVideoPositionUseCase(),
                mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
                resolveTimelineOverlap = ResolveTimelineOverlapUseCase(),
            ),
            scope = CoroutineScope(context = testScope.backgroundScope.coroutineContext + dispatcher),
            defaultDispatcher = dispatcher,
            screenClockDispatcher = dispatcher,
        )
    }

    private companion object {
        val SAMPLE_FILES = listOf("network.txt", "device-ui.txt", "backend-service.txt")
        const val CLIP_NAME = "sample-clip.mp4"
        const val EXOTIC_NAME = "analytics-custom.txt"
        const val MIN_EXPECTED_RECORDS = 300
        const val VIDEO_DURATION_MILLIS = 120_000L
        const val ANCHOR_POSITION_MILLIS = 30_000L
    }
}
