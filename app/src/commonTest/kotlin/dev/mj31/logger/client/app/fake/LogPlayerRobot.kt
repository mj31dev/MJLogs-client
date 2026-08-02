package dev.mj31.logger.client.app.fake

import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerRepositories
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerUseCases
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.data.repository.InMemoryLogSessionRepository
import dev.mj31.logger.client.data.repository.InMemorySyncRepository
import dev.mj31.logger.client.data.repository.InMemoryVideoRepository
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.TimeZone
import dev.mj31.logger.client.app.usecase.sync.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapLogTimeToVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.sync.ClearSynchronizationUseCase
import dev.mj31.logger.client.app.usecase.session.FilterLogEntriesUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParserFactory
import dev.mj31.logger.client.app.fake.format.FakeLogFormatDetector
import dev.mj31.logger.client.data.format.preview.RegexLogFormatPreviewer
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerStateAssembler
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.fake.source.FixedIdGenerator
import dev.mj31.logger.client.app.fake.source.FixedClock
import dev.mj31.logger.client.app.fake.source.FakeTextFileDataSource
import dev.mj31.logger.client.app.fake.format.FakeLogFormatCompiler
import dev.mj31.logger.client.app.usecase.sync.ParseFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.SynchronizeAtTimestampUseCase
import dev.mj31.logger.client.app.usecase.sync.ComposeFrameTimeUseCase

/**
 * Assembles a [LogPlayerStore] with the real in-memory repositories and the real use cases,
 * replacing only the boundaries a unit test cannot own: the file system, the video engine and the
 * format detection heuristics.
 *
 * A test reads as a sequence of dispatched intents followed by assertions on [state] and on the
 * [effects] the store emitted; every action drains the test scheduler before and after the call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogPlayerRobot private constructor(
    private val testScope: TestScope,
    val store: LogPlayerStore,
    private val recordedEffects: MutableList<LogPlayerEffect>,
    val player: FakeVideoPlayer,
    val files: FakeTextFileDataSource,
    val detector: FakeLogFormatDetector,
    val compiler: FakeLogFormatCompiler,
    val sessionRepository: InMemoryLogSessionRepository,
    val videoRepository: InMemoryVideoRepository,
    val syncRepository: InMemorySyncRepository,
) {

    /** Latest rendered snapshot, after every pending coroutine has run. */
    val state: LogPlayerState
        get() {
            settle()
            return store.state.value
        }

    /** Every effect emitted so far, in order. */
    val effects: List<LogPlayerEffect>
        get() {
            settle()
            return recordedEffects.toList()
        }

    /** Transient notifications emitted so far, still unresolved so a locale cannot break a test. */
    val messages: List<UiText>
        get() = effects.filterIsInstance<LogPlayerEffect.ShowMessage>().map { it.text }

    val lastMessage: UiText?
        get() = messages.lastOrNull()

    /** Values the last message was formatted with, such as the imported file name. */
    val lastMessageArguments: List<Any>
        get() = (lastMessage as? UiText.Resource)?.arguments.orEmpty()

    fun settle() {
        testScope.testScheduler.advanceUntilIdle()
    }

    fun dispatch(intent: LogPlayerIntent) = act { store.handleIntent(intent = intent) }

    fun importLogFiles(paths: List<String>) = dispatch(intent = LogPlayerIntent.ImportLogFiles(paths = paths))

    fun importVideo(path: String = DEFAULT_VIDEO_PATH) = dispatch(intent = LogPlayerIntent.ImportVideo(path = path))

    fun updateFormatDraft(
        timestampPattern: String = DEFAULT_TIMESTAMP_PATTERN,
        structureTemplate: String = DEFAULT_STRUCTURE_TEMPLATE,
    ) = dispatch(
        intent = LogPlayerIntent.UpdateFormatDraft(
            timestampPattern = timestampPattern,
            structureTemplate = structureTemplate,
        ),
    )

    /** Types a format into the dialog and applies it, the way the user does. */
    fun submitManualFormat(
        timestampPattern: String = DEFAULT_TIMESTAMP_PATTERN,
        structureTemplate: String = DEFAULT_STRUCTURE_TEMPLATE,
    ) {
        updateFormatDraft(timestampPattern = timestampPattern, structureTemplate = structureTemplate)
        dispatch(intent = LogPlayerIntent.SubmitManualFormat)
    }

    fun dismissFormatRequest() = dispatch(intent = LogPlayerIntent.DismissFormatRequest)

    fun updateFilter(filter: LogFilter) = dispatch(intent = LogPlayerIntent.UpdateFilter(filter = filter))

    fun setTimeWindow(windowMillis: Long?) = dispatch(intent = LogPlayerIntent.SetTimeWindow(windowMillis = windowMillis))

    fun selectEntry(entryId: String?) = dispatch(intent = LogPlayerIntent.SelectEntry(entryId = entryId))

    fun synchronize() = dispatch(intent = LogPlayerIntent.Synchronize)

    fun typeFrameTime(text: String) = dispatch(intent = LogPlayerIntent.UpdateFrameTime(text = text))

    fun synchronizeAtFrameTime() = dispatch(intent = LogPlayerIntent.SynchronizeAtFrameTime)

    fun pickFrameTime(dateMillis: Long, hour: Int, minute: Int) =
        dispatch(intent = LogPlayerIntent.PickFrameTime(dateMillis = dateMillis, hour = hour, minute = minute))

    fun clearSynchronization() = dispatch(intent = LogPlayerIntent.ClearSynchronization)

    fun setFollowVideo(enabled: Boolean) = dispatch(intent = LogPlayerIntent.SetFollowVideo(enabled = enabled))

    /** Moves the video playhead the way playback progress would. */
    fun movePlayheadTo(positionMillis: Long) = act { player.setPosition(positionMillis = positionMillis) }

    /** Imports both fixture files with a successfully detected format each. */
    fun importBothLogFiles() {
        detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        detector.enqueueDetected(spec = LogPlayerFixtures.SECOND_SPEC)
        importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH, LogPlayerFixtures.SECOND_PATH))
    }

    /** Loads a screencast of [durationMillis] and parks the playhead at [positionMillis]. */
    fun loadVideo(durationMillis: Long = DEFAULT_VIDEO_DURATION_MILLIS, positionMillis: Long = 0L) {
        importVideo()
        act {
            player.setDuration(durationMillis = durationMillis)
            player.setPosition(positionMillis = positionMillis)
        }
    }

    private inline fun act(block: () -> Unit) {
        settle()
        block()
        settle()
    }

    companion object {

        const val DEFAULT_VIDEO_PATH: String = "/media/screencast.mp4"
        const val DEFAULT_VIDEO_NAME: String = "screencast.mp4"
        const val DEFAULT_VIDEO_DURATION_MILLIS: Long = 60_000L
        const val DEFAULT_TIMESTAMP_PATTERN: String = "epochMillis"
        const val DEFAULT_STRUCTURE_TEMPLATE: String = "{timestamp}|{level}|{tag}|{message}"

        fun create(testScope: TestScope): LogPlayerRobot {
            val dispatcher = UnconfinedTestDispatcher(scheduler = testScope.testScheduler)
            val scope = CoroutineScope(context = testScope.backgroundScope.coroutineContext + dispatcher)

            val files = FakeTextFileDataSource()
            files.register(content = LogPlayerFixtures.firstFile)
            files.register(content = LogPlayerFixtures.secondFile)
            files.register(content = LogPlayerFixtures.unparsableFile)

            val loader = LogSourceLoader(
                dataSource = files,
                assembler = LogSourceAssembler(parserFactory = ScriptedLogLineParserFactory()),
                idGenerator = FixedIdGenerator(),
                clock = FixedClock(instant = LogPlayerFixtures.BASE),
                timeZone = TimeZone.UTC,
            )
            val detector = FakeLogFormatDetector()
            val compiler = FakeLogFormatCompiler(
                result = FormatCompilationResult.Success(spec = LogPlayerFixtures.MANUAL_SPEC),
            )
            val player = FakeVideoPlayer()
            val sessionRepository = InMemoryLogSessionRepository()
            val videoRepository = InMemoryVideoRepository()
            val syncRepository = InMemorySyncRepository()

            val store = buildStore(
                loader = loader,
                detector = detector,
                compiler = compiler,
                player = player,
                repositories = LogPlayerRepositories(
                    session = sessionRepository,
                    video = videoRepository,
                    sync = syncRepository,
                ),
                dispatcher = dispatcher,
                scope = scope,
            )

            val recordedEffects = mutableListOf<LogPlayerEffect>()
            testScope.backgroundScope.launch(context = dispatcher) {
                store.effects.collect { effect -> recordedEffects += effect }
            }

            return LogPlayerRobot(
                testScope = testScope,
                store = store,
                recordedEffects = recordedEffects,
                player = player,
                files = files,
                detector = detector,
                compiler = compiler,
                sessionRepository = sessionRepository,
                videoRepository = videoRepository,
                syncRepository = syncRepository,
            )
        }

        private fun buildStore(
            loader: LogSourceLoader,
            detector: FakeLogFormatDetector,
            compiler: FakeLogFormatCompiler,
            player: FakeVideoPlayer,
            repositories: LogPlayerRepositories,
            dispatcher: CoroutineDispatcher,
            scope: CoroutineScope,
        ): LogPlayerStore {
            val parseFrameTime = ParseFrameTimeUseCase()
            return LogPlayerStore(
            repositories = repositories,
            useCases = LogPlayerUseCases(
                mergeLogSources = MergeLogSourcesUseCase(),
                importLogFile = ImportLogFileUseCase(
                    loader = loader,
                    detector = detector,
                    dispatcher = dispatcher,
                ),
                importLogFileWithFormat = ImportLogFileWithFormatUseCase(
                    loader = loader,
                    dispatcher = dispatcher,
                ),
                filterLogEntries = FilterLogEntriesUseCase(),
                synchronizeTimelines = SynchronizeTimelinesUseCase(syncRepository = repositories.sync),
                synchronizeAtTimestamp = SynchronizeAtTimestampUseCase(syncRepository = repositories.sync),
                parseFrameTime = parseFrameTime,
                composeFrameTime = ComposeFrameTimeUseCase(parseFrameTime = parseFrameTime),
                clearSynchronization = ClearSynchronizationUseCase(syncRepository = repositories.sync),
                mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
                mapLogTimeToVideoPosition = MapLogTimeToVideoPositionUseCase(),
            ),
            player = player,
            formatTools = LogPlayerFormatTools(compiler = compiler, previewer = RegexLogFormatPreviewer()),
            stateAssembler = LogPlayerStateAssembler(
                parseFrameTime = parseFrameTime,
                findEntryAtVideoPosition = FindEntryAtVideoPositionUseCase(),
                mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
                resolveTimelineOverlap = ResolveTimelineOverlapUseCase(),
            ),
            scope = scope,
            defaultDispatcher = dispatcher,
        )
        }

    }
}
