package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerRepositories
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerUseCases
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerWorkspace
import dev.mj31.logger.client.app.features.sessions.SessionsStore
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.workspace.CaptureWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.ClearWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.PersistWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.RestoreWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.CloseSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.OpenSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.SaveSessionPackageUseCase
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import dev.mj31.logger.client.domain.session.SessionPackageStore
import kotlin.time.Clock
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.preview.LogFormatPreviewer
import dev.mj31.logger.client.domain.player.VideoPlayer
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeAtTimestampUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapLogTimeToVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ClearSynchronizationUseCase
import dev.mj31.logger.client.app.usecase.session.FilterLogEntriesUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerStateAssembler
import dev.mj31.logger.client.app.usecase.sync.manual.ComposeFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.AutoSynchronizeUseCase
import dev.mj31.logger.client.app.usecase.playback.StepVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.metadata.MetadataAnchorUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.FindMinuteChangeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.LocateClockRegionUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.ReadClockUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.zone.ResolveClockAnchorUseCase
import dev.mj31.logger.client.domain.source.video.VideoFrameScanner
import dev.mj31.logger.client.domain.source.video.VideoMetadataSource
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader

/** The MVI store and everything it is assembled from. */
interface PresentationBindings {

    @Provides
    fun repositories(
        session: LogSessionRepository,
        video: VideoRepository,
        sync: SyncRepository,
    ): LogPlayerRepositories = LogPlayerRepositories(session = session, video = video, sync = sync)

    @Provides
    fun useCases(
        mergeLogSources: MergeLogSourcesUseCase,
        parseFrameTime: ParseFrameTimeUseCase,
        importLogFile: ImportLogFileUseCase,
        importLogFileWithFormat: ImportLogFileWithFormatUseCase,
        autoSynchronize: AutoSynchronizeUseCase,
        syncRepository: SyncRepository,
    ): LogPlayerUseCases = LogPlayerUseCases(
        autoSynchronize = autoSynchronize,
        mergeLogSources = mergeLogSources,
        importLogFile = importLogFile,
        importLogFileWithFormat = importLogFileWithFormat,
        filterLogEntries = FilterLogEntriesUseCase(),
        synchronizeTimelines = SynchronizeTimelinesUseCase(syncRepository = syncRepository),
        synchronizeAtTimestamp = SynchronizeAtTimestampUseCase(syncRepository = syncRepository),
        parseFrameTime = parseFrameTime,
        composeFrameTime = ComposeFrameTimeUseCase(parseFrameTime = parseFrameTime),
        stepVideoPosition = StepVideoPositionUseCase(),
        clearSynchronization = ClearSynchronizationUseCase(syncRepository = syncRepository),
        mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
        mapLogTimeToVideoPosition = MapLogTimeToVideoPositionUseCase(),
    )

    @Provides
    fun formatTools(
        compiler: LogFormatCompiler,
        previewer: LogFormatPreviewer,
    ): LogPlayerFormatTools = LogPlayerFormatTools(compiler = compiler, previewer = previewer)

    /**
     * The whole persistence contour.
     *
     * It is assembled here rather than injected piece by piece because every one of these use cases
     * exists only to serve the player, and none of them is useful on its own.
     */
    @Provides
    fun workspace(
        workspaceRepository: WorkspaceRepository,
        packageStore: SessionPackageStore,
        sessionRepository: LogSessionRepository,
        videoRepository: VideoRepository,
        syncRepository: SyncRepository,
        loader: LogSourceLoader,
        clock: Clock,
    ): LogPlayerWorkspace {
        val restore = RestoreWorkspaceUseCase(
            loader = loader,
            sessionRepository = sessionRepository,
            videoRepository = videoRepository,
            syncRepository = syncRepository,
        )
        val persist = PersistWorkspaceUseCase(
            workspaceRepository = workspaceRepository,
            packageStore = packageStore,
        )
        val closePackage = CloseSessionPackageUseCase(
            packageStore = packageStore,
            persistWorkspace = persist,
        )
        return LogPlayerWorkspace(
            repository = workspaceRepository,
            capture = CaptureWorkspaceUseCase(
                sessionRepository = sessionRepository,
                videoRepository = videoRepository,
                syncRepository = syncRepository,
            ),
            restore = restore,
            clear = ClearWorkspaceUseCase(
                closeSessionPackage = closePackage,
                sessionRepository = sessionRepository,
                videoRepository = videoRepository,
                syncRepository = syncRepository,
                workspaceRepository = workspaceRepository,
            ),
            persist = persist,
            savePackage = SaveSessionPackageUseCase(
                packageStore = packageStore,
                workspaceRepository = workspaceRepository,
                clock = clock,
            ),
            openPackage = OpenSessionPackageUseCase(
                packageStore = packageStore,
                restoreWorkspace = restore,
                workspaceRepository = workspaceRepository,
                clock = clock,
            ),
            closePackage = closePackage,
        )
    }

    @Provides
    fun parseFrameTime(): ParseFrameTimeUseCase = ParseFrameTimeUseCase()

    /**
     * The whole automatic contour, assembled here because every piece of it is stateless except the
     * recognizer, which the platform component owns.
     */
    @Provides
    fun autoSynchronize(
        metadataSource: VideoMetadataSource,
        scanner: VideoFrameScanner,
        clockReader: ScreenClockReader,
        syncRepository: SyncRepository,
    ): AutoSynchronizeUseCase {
        val readClock = ReadClockUseCase(reader = clockReader)
        return AutoSynchronizeUseCase(
            metadataAnchor = MetadataAnchorUseCase(metadataSource = metadataSource),
            scanner = scanner,
            clockReader = clockReader,
            locateClockRegion = LocateClockRegionUseCase(readClock = readClock),
            findMinuteChange = FindMinuteChangeUseCase(readClock = readClock),
            resolveClockAnchor = ResolveClockAnchorUseCase(),
            syncRepository = syncRepository,
        )
    }

    @Provides
    fun stateAssembler(parseFrameTime: ParseFrameTimeUseCase): LogPlayerStateAssembler = LogPlayerStateAssembler(
        parseFrameTime = parseFrameTime,
        findEntryAtVideoPosition = FindEntryAtVideoPositionUseCase(),
        mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
        resolveTimelineOverlap = ResolveTimelineOverlapUseCase(),
    )

    @AppScope
    @Provides
    fun sessionsStore(
        workspaceRepository: WorkspaceRepository,
        scope: CoroutineScope,
    ): SessionsStore = SessionsStore(repository = workspaceRepository, scope = scope)

    @AppScope
    @Provides
    fun store(
        repositories: LogPlayerRepositories,
        useCases: LogPlayerUseCases,
        player: VideoPlayer,
        formatTools: LogPlayerFormatTools,
        stateAssembler: LogPlayerStateAssembler,
        scope: CoroutineScope,
        dispatcher: DefaultDispatcher,
        screenClockDispatcher: ScreenClockDispatcher,
        workspace: LogPlayerWorkspace,
    ): LogPlayerStore = LogPlayerStore(
        screenClockDispatcher = screenClockDispatcher,
        repositories = repositories,
        useCases = useCases,
        player = player,
        formatTools = formatTools,
        stateAssembler = stateAssembler,
        scope = scope,
        defaultDispatcher = dispatcher,
        workspace = workspace,
    )
}
