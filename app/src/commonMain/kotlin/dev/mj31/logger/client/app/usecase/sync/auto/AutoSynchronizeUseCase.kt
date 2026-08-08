package dev.mj31.logger.client.app.usecase.sync.auto

import dev.mj31.logger.client.app.usecase.sync.auto.metadata.MetadataAnchorUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.FindMinuteChangeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.LocateClockRegionUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.zone.ResolveClockAnchorUseCase
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.source.video.VideoFrameScanner
import dev.mj31.logger.client.domain.source.video.VideoScan
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader

/**
 * Synchronizes the two timelines without asking the user anything.
 *
 * There are two ways to find out when a recording was made, and they are tried in that order because
 * they cost wildly different amounts. The container's own creation time is a header read; the clock
 * the recording displays costs opening a second decoder and putting a few dozen frames through a
 * recognizer. So metadata answers first, and the picture is only consulted when metadata has nothing
 * to say or says something the loaded logs contradict.
 *
 * The two are not equal in what they buy, either. Metadata locates the recording to about a second.
 * The frame on which the clock changed minute locates it to a frame — which is why [refine] exists
 * as a separate entry point: once metadata has produced a serviceable anchor, sharpening it by two
 * orders of magnitude is worth doing, but not worth doing unasked.
 *
 * Nothing here decides what to show. Each path ends in an [AutoSyncOutcome], and an anchor reaches
 * the repository only once a moment has actually been established — the ways of failing to establish
 * one are distinguished from each other, because they call for different things from the user.
 */
class AutoSynchronizeUseCase(
    private val metadataAnchor: MetadataAnchorUseCase,
    private val scanner: VideoFrameScanner,
    private val clockReader: ScreenClockReader,
    private val locateClockRegion: LocateClockRegionUseCase,
    private val findMinuteChange: FindMinuteChangeUseCase,
    private val resolveClockAnchor: ResolveClockAnchorUseCase,
    private val syncRepository: SyncRepository,
) {

    /** The cascade run when a screencast and logs first meet: metadata, then the picture. */
    suspend fun automatic(media: VideoMedia, session: LogSession): AutoSyncOutcome {
        val fromMetadata = metadataAnchor(media = media, logRange = session.timeRange)
        if (fromMetadata != null) return apply(anchor = fromMetadata)
        return refine(media = media, session = session, region = null)
    }

    /**
     * Reads the clock off the picture, either because metadata failed or because the user asked for
     * an anchor sharper than metadata can give.
     *
     * [region] is what the user pointed at, when they had to; `null` means look for it.
     */
    suspend fun refine(media: VideoMedia, session: LogSession, region: ClockRegion?): AutoSyncOutcome {
        if (session.timeRange == null) return AutoSyncOutcome.NothingToCorrelate
        if (!clockReader.isAvailable) return AutoSyncOutcome.RecognizerMissing
        val scan = scanner.open(media = media) ?: return AutoSyncOutcome.VideoUnreadable
        return try {
            fromScreenClock(scan = scan, session = session, region = region)
        } finally {
            scan.close()
        }
    }

    private suspend fun fromScreenClock(
        scan: VideoScan,
        session: LogSession,
        region: ClockRegion?,
    ): AutoSyncOutcome {
        // Where the clock sits is looked for across the whole recording; when it changed minute is
        // hunted only in the opening minute, which is all the anchor needs.
        val located = region ?: locateClockRegion(scan = scan, spanMillis = scan.durationMillis)
            ?: return AutoSyncOutcome.ClockNotFound

        val change = findMinuteChange(scan = scan, region = located) ?: return AutoSyncOutcome.NoMinuteChange

        return apply(
            anchor = resolveClockAnchor(
                boundary = change.reading,
                logRange = session.timeRange,
                videoDurationMillis = scan.durationMillis,
                accuracyMillis = change.accuracyMillis,
            ),
        )
    }

    private suspend fun apply(anchor: SyncAnchor): AutoSyncOutcome {
        syncRepository.setAnchor(anchor = anchor)
        return AutoSyncOutcome.Synchronized(anchor = anchor)
    }
}
