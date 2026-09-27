package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.app.features.logplayer.state.ingest.SourcePreambleUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.app.usecase.sync.ResolveSyncZoneUseCase
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.domain.sync.SyncState
import dev.mj31.logger.client.app.features.logplayer.state.ui.AutoSyncUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.SyncUiState
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Builds the immutable [LogPlayerState] out of the repository streams and the local state.
 *
 * Extracted from the view model so that the (non trivial) derivation of the active record and of
 * the timeline overlap can be unit tested in isolation.
 */
class LogPlayerStateAssembler(
    private val parseFrameTime: ParseFrameTimeUseCase,
    private val findEntryAtVideoPosition: FindEntryAtVideoPositionUseCase,
    private val mapVideoPositionToLogTime: MapVideoPositionToLogTimeUseCase,
    private val resolveTimelineOverlap: ResolveTimelineOverlapUseCase,
    private val resolveSyncZone: ResolveSyncZoneUseCase,
) {

    fun assemble(
        session: LogSession,
        visibleEntries: List<LogEntry>,
        video: VideoSnapshot,
        syncState: SyncState,
        local: LogPlayerLocalState,
    ): LogPlayerState {
        val anchor = syncState.anchorOrNull
        val playback = video.playback
        val syncZone = resolveSyncZone(
            sources = session.sources,
            selectedSourceId = local.selectedSourceId,
            chosenZoneId = local.frameTimeZoneId,
        )
        val activeEntry = anchor?.let {
            findEntryAtVideoPosition(
                entries = visibleEntries,
                anchor = it,
                videoPositionMillis = playback.positionMillis,
            )
        }
        val videoState = VideoUiState(
            name = video.media?.name,
            status = playback.status,
            positionMillis = playback.positionMillis,
            durationMillis = playback.durationMillis,
            errorMessage = playback.errorMessage,
        )

        return LogPlayerState(
            sources = sourcesOf(session = session, local = local),
            entries = visibleEntries,
            totalEntryCount = session.entries.size,
            filter = local.filter,
            timeWindowMillis = local.timeWindowMillis,
            selectedEntryId = local.selectedEntryId,
            activeEntryId = activeEntry?.id,
            followVideo = local.followVideo,
            video = videoState,
            sync = syncStateOf(
                session = session,
                syncState = syncState,
                playback = playback,
                hasVideo = videoState.hasVideo,
                local = local,
                zone = syncZone,
            ),
            autoSync = autoSyncStateOf(session = session, video = videoState, anchor = anchor, local = local),
            formatRequest = local.formatRequests.firstOrNull(),
            unsupportedImport = local.unsupportedImports.firstOrNull(),
            startDayRequest = local.startDayRequests.firstOrNull(),
            isImporting = local.isImporting,
            preamble = preambleOf(session = session, local = local),
            duplicateRequest = local.duplicateRequests.firstOrNull(),
            zoneRequest = zoneRequestOf(session = session, local = local, syncZone = syncZone),
            sourceTimeZones = session.sources.associate { it.id to it.zone.timeZone },
            showUtcColumn = session.sources.any { !it.zone.isUtc },
            workspace = local.workspace,
        )
    }

    @Suppress("LongParameterList")
    private fun syncStateOf(
        session: LogSession,
        syncState: SyncState,
        playback: PlaybackState,
        hasVideo: Boolean,
        local: LogPlayerLocalState,
        zone: SourceZone,
    ): SyncUiState {
        val anchor = syncState.anchorOrNull
        return SyncUiState(
        isSynced = syncState.isSynced,
        origin = anchor?.origin,
        accuracyMillis = anchor?.accuracyMillis ?: 0L,
        anchorEntryId = anchor?.logEntryId,
        anchorVideoPositionMillis = anchor?.videoPositionMillis ?: 0L,
        logTimeAtPlayhead = anchor?.let {
            mapVideoPositionToLogTime(anchor = it, videoPositionMillis = playback.positionMillis)
        },
        overlap = resolveTimelineOverlap(
            logRange = session.timeRange,
            anchor = anchor,
            videoDurationMillis = playback.durationMillis,
        ),
        canSynchronize = local.selectedEntryId != null && hasVideo,
        frameTime = local.frameTime,
        frameTimeError = local.frameTimeError,
        canSynchronizeAtFrameTime = local.frameTime.isNotBlank() && hasVideo,
        frameTimeDefault = frameTimeDefaultOf(session = session, local = local, zone = zone),
        zone = zone,
    )
    }

    /** Gone as soon as its file is: a preamble outliving the source it was read from would be a stale view. */
    private fun preambleOf(session: LogSession, local: LogPlayerLocalState): SourcePreambleUiState? {
        val id = local.preambleSourceId ?: return null
        val source = session.sources.firstOrNull { it.id == id } ?: return null
        return SourcePreambleUiState(sourceId = source.id, fileName = source.name, lines = source.preamble)
    }

    /** Gone with its file, like the preamble: a choice for a file no longer loaded applies to nothing. */
    private fun zoneRequestOf(session: LogSession, local: LogPlayerLocalState, syncZone: SourceZone): ZoneRequestUiState? =
        when (val target = local.zoneRequest) {
            null -> null
            ZoneTarget.FrameTime -> ZoneRequestUiState(
                target = target,
                subject = null,
                current = syncZone,
                availableZoneIds = AVAILABLE_ZONE_IDS,
            )
            is ZoneTarget.Source -> session.sourceById(sourceId = target.sourceId)?.let { source ->
                ZoneRequestUiState(
                    target = target,
                    subject = source.name,
                    current = source.zone,
                    availableZoneIds = AVAILABLE_ZONE_IDS,
                )
            }
        }

    /** An empty selection means every file is shown, which is not the same as none being chosen. */
    private fun sourcesOf(session: LogSession, local: LogPlayerLocalState): List<LogSourceUi> =
        session.sources.map { source ->
            LogSourceUi(
                id = source.id,
                name = source.name,
                formatName = source.format.name,
                entryCount = source.entryCount,
                skippedLineCount = source.skippedLineCount,
                isSelected = local.filter.sourceIds.isEmpty() || source.id in local.filter.sourceIds,
                hasPreamble = source.preamble.isNotEmpty(),
                zone = source.zone,
                fileCount = source.paths.size,
            )
        }

    /**
     * Refining is offered only against an anchor that has room to improve.
     *
     * A creation time locates the recording to about a second; the frame a clock changed minute
     * locates it to a frame. Offering to sharpen an anchor a human placed, or one already read off
     * the screen, would be offering to redo work that is already exact.
     */
    private fun autoSyncStateOf(
        session: LogSession,
        video: VideoUiState,
        anchor: SyncAnchor?,
        local: LogPlayerLocalState,
    ): AutoSyncUiState = AutoSyncUiState(
        isScanning = local.isScanningClock,
        canRun = video.hasVideo && !session.isEmpty && !local.isScanningClock,
        canRefine = video.hasVideo && !local.isScanningClock && anchor?.origin == SyncOrigin.VIDEO_METADATA,
        isSelectingRegion = local.isSelectingClockRegion,
    )

    /**
     * Moment the date and time picker opens on: what the field already says when it can be read,
     * otherwise the beginning of the loaded session, which is the day the recording belongs to.
     */
    private fun frameTimeDefaultOf(session: LogSession, local: LogPlayerLocalState, zone: SourceZone): Instant? {
        val sessionStart = session.timeRange?.start
        val referenceDate = sessionStart?.toLocalDateTime(timeZone = zone.timeZone)?.date
        return parseFrameTime(text = local.frameTime, referenceDate = referenceDate, timeZone = zone.timeZone)
            ?: sessionStart
    }

    private companion object {

        /** Every region the platform knows, read once: the list does not change while the app runs. */
        val AVAILABLE_ZONE_IDS: List<String> by lazy {
            TimeZone.availableZoneIds.filter { '/' in it && !it.startsWith(prefix = "Etc/") }.sorted()
        }
    }
}
