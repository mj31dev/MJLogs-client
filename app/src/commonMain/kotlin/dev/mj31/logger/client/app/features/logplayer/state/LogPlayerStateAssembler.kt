package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.sync.SyncState
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.SyncUiState
import dev.mj31.logger.client.app.usecase.sync.ParseFrameTimeUseCase
import kotlinx.datetime.Instant
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
            sources = session.sources.map { source ->
                LogSourceUi(
                    id = source.id,
                    name = source.name,
                    formatName = source.format.name,
                    entryCount = source.entryCount,
                    skippedLineCount = source.skippedLineCount,
                    isSelected = local.filter.sourceIds.isEmpty() || source.id in local.filter.sourceIds,
                )
            },
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
            formatRequest = local.formatRequests.firstOrNull(),
            isImporting = local.isImporting,
        )
    }

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
