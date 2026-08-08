package dev.mj31.logger.client.app.usecase.sync.auto.metadata

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.source.video.VideoMetadataSource
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Builds an anchor out of what the video file says about itself, and refuses to when it does not
 * hold up.
 *
 * This is the cheap path, and it is the first one tried: a header read, no decoding, an answer in
 * milliseconds. The price is that the answer is often wrong. A recorder writes the moment recording
 * started into a field defined as UTC, and phones routinely write local time there instead; a file
 * that has been through a re-encode carries the moment of the re-encode.
 *
 * The check is what makes the path usable: the recording is claimed to run from its creation moment
 * for its own duration, and most of that has to fall inside the logs the user actually loaded. Half
 * is the bar. Demanding the whole of it would reject the ordinary case where a tester keeps
 * recording after the log ends, and demanding merely a touch would accept a file whose clock is an
 * hour out, which is exactly the failure worth catching.
 */
class MetadataAnchorUseCase(
    private val metadataSource: VideoMetadataSource,
) {

    suspend operator fun invoke(media: VideoMedia, logRange: TimeRange?): SyncAnchor? {
        if (logRange == null) return null
        val metadata = metadataSource.read(media = media) ?: return null
        val created = metadata.creationTime ?: return null
        if (metadata.durationMillis <= 0L) return null

        val covered = TimeRange(start = created, end = created + metadata.durationMillis.milliseconds)
        if (!overlaps(covered = covered, logRange = logRange)) return null

        return SyncAnchor(
            logTimestamp = created,
            videoPositionMillis = 0L,
            origin = SyncOrigin.VIDEO_METADATA,
            accuracyMillis = ACCURACY_MILLIS,
        )
    }

    private fun overlaps(covered: TimeRange, logRange: TimeRange): Boolean {
        val start = maxOf(a = covered.start, b = logRange.start)
        val end = minOf(a = covered.end, b = logRange.end)
        val shared = end.toEpochMilliseconds() - start.toEpochMilliseconds()
        return shared * REQUIRED_SHARE_DIVISOR >= covered.durationMillis
    }

    private companion object {
        /**
         * A creation time is stamped when recording starts, not when the first frame is shown, and
         * neither the recorder nor the device clock is exact to better than about a second.
         */
        const val ACCURACY_MILLIS = 1_000L

        /** Half of the recording, expressed so that the comparison stays in whole numbers. */
        const val REQUIRED_SHARE_DIVISOR = 2
    }
}
