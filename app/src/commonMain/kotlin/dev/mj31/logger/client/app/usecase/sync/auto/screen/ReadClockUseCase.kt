package dev.mj31.logger.client.app.usecase.sync.auto.screen

import dev.mj31.logger.client.domain.source.video.VideoScan
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader
import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime

/**
 * Reads the clock at one point of a recording, by asking more than once.
 *
 * A single recognition is not evidence. On a real screen recording roughly one frame in four holds
 * no readable clock at all — a screen is fading into another, a light dialog is sitting over a light
 * status bar — and among the frames that do read, one occasionally reads wrong while looking
 * entirely plausible. Three frames a few dozen milliseconds apart cost almost nothing extra, because
 * the decoder has already seeked to the neighbourhood, and two of them agreeing is worth far more
 * than one of them speaking.
 *
 * [spreadMillis] is how far apart those frames are taken, and it is the caller's decision: a search
 * for the region can afford to sample across half a second, while the hunt for the exact frame a
 * minute changed cannot blur its own answer.
 *
 * The samples straddle the position rather than following it. Reading only forwards makes the answer
 * lean forwards too — right beside a change, two of the three frames already show the new minute and
 * the vote reports it before it happened, by as much as the whole spread.
 */
class ReadClockUseCase(
    private val reader: ScreenClockReader,
) {

    suspend operator fun invoke(
        scan: VideoScan,
        region: ClockRegion,
        positionMillis: Long,
        spreadMillis: Long = DEFAULT_SPREAD_MILLIS,
    ): ScreenClockTime? {
        val readings = OFFSETS.mapNotNull { offset ->
            val frame = scan.frameAt(
                positionMillis = (positionMillis + offset * spreadMillis).coerceAtLeast(minimumValue = 0L),
            )
            frame?.let { reader.read(frame = it, region = region) }
        }
        return readings.groupingBy { it }.eachCount()
            .filterValues { count -> count >= AGREEING }
            .maxByOrNull { entry -> entry.value }
            ?.key
    }

    private companion object {
        const val AGREEING = 2
        const val DEFAULT_SPREAD_MILLIS = 400L

        /** One frame each side of the position, and the position itself. */
        val OFFSETS = listOf(-1, 0, 1)
    }
}
