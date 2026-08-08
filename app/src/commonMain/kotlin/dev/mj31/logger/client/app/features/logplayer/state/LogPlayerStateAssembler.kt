package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSession
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
            sync = SyncUiState(
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
                canSynchronize = local.selectedEntryId != null && videoState.hasVideo,
                frameTime = local.frameTime,
                frameTimeError = local.frameTimeError,
                canSynchronizeAtFrameTime = local.frameTime.isNotBlank() && videoState.hasVideo,
                frameTimeDefault = frameTimeDefaultOf(session = session, local = local),
            ),
            autoSync = autoSyncStateOf(session = session, video = videoState, anchor = anchor, local = local),
            formatRequest = local.formatRequests.firstOrNull(),
            isImporting = local.isImporting,
        )
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
    private fun frameTimeDefaultOf(session: LogSession, local: LogPlayerLocalState): Instant? {
        val sessionStart = session.timeRange?.start
        val referenceDate = sessionStart?.toLocalDateTime(timeZone = TimeZone.UTC)?.date
        return parseFrameTime(text = local.frameTime, referenceDate = referenceDate) ?: sessionStart
    }
}
