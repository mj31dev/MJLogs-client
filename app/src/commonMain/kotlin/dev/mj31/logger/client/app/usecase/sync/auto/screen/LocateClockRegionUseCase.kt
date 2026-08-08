package dev.mj31.logger.client.app.usecase.sync.auto.screen

import dev.mj31.logger.client.domain.source.video.VideoScan
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ClockRegionPresets
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReading

/**
 * Finds which band of the frame holds the clock.
 *
 * The tempting rule — take the first band that yields a time — is wrong, and measurably so. Point a
 * recognizer at something that is not a clock and it does not fall silent: it returns `01:09` for a
 * bar plainly showing `09:28`, confidently and repeatedly. Repetition proves nothing either, because
 * the same crop misreads the same way on every frame of a scene.
 *
 * What a wrong band cannot fake is **the passage of time**. So each candidate is read at several
 * points across the recording, and the question asked of the results is whether one clock could have
 * produced them: a chain of readings that never goes backwards and never gains more minutes than the
 * recording has been running. The longest such chain is the band's evidence, and most of the
 * readings have to belong to it.
 *
 * Demanding that *every* reading fit would be too much. A correct band still misreads occasionally —
 * the sample recording this was built against reads `09:25` for several seconds in the middle of a
 * stretch of `09:28` — and a rule that a single bad frame can veto finds nothing at all. Demanding a
 * majority keeps the outlier out of the chain instead of out of the answer.
 *
 * There is no blind sweep of the rest of the frame. A clock is in a status bar or it is nowhere the
 * application can be expected to guess, and the user pointing at it costs them one drag and costs a
 * search of the whole picture nothing at all.
 */
class LocateClockRegionUseCase(
    private val readClock: ReadClockUseCase,
) {

    suspend operator fun invoke(scan: VideoScan, spanMillis: Long): ClockRegion? {
        val positions = probePositions(spanMillis = spanMillis)
        return ClockRegionPresets.ordered.firstOrNull { band ->
            longestClockChain(readings = read(scan = scan, region = band, positions = positions)) >= MINIMUM_CHAIN
        }
    }

    /**
     * Reads every probe position, and does not give up early on the blank ones.
     *
     * A blank is not evidence against a band, only the absence of evidence for it: a quarter of the
     * frames of a real recording hold no readable clock at all, because a screen is fading into
     * another or a light sheet is sitting over a light status bar. Abandoning a band on the third
     * blank is what made the search give up on ordinary recordings of ordinary app use, where the
     * opening seconds are the busiest part of the whole file.
     */
    private suspend fun read(
        scan: VideoScan,
        region: ClockRegion,
        positions: List<Long>,
    ): List<ScreenClockReading> = positions.mapNotNull { position ->
        readClock(scan = scan, region = region, positionMillis = position)
            ?.let { clock -> ScreenClockReading(positionMillis = position, time = clock.time) }
    }

    /**
     * The longest run of readings one clock could have produced.
     *
     * Two readings belong to the same clock when the later one is not earlier on the dial, and has
     * not gained more minutes than the recording ran between them — with a minute and a half of
     * slack, since both readings are rounded to a whole minute.
     */
    private fun longestClockChain(readings: List<ScreenClockReading>): Int {
        val ordered = readings.sortedBy { it.positionMillis }
        val longest = IntArray(size = ordered.size) { 1 }
        for (later in ordered.indices) {
            for (earlier in 0 until later) {
                if (couldFollow(earlier = ordered[earlier], later = ordered[later])) {
                    longest[later] = maxOf(a = longest[later], b = longest[earlier] + 1)
                }
            }
        }
        return longest.maxOrNull() ?: 0
    }

    private fun couldFollow(earlier: ScreenClockReading, later: ScreenClockReading): Boolean {
        val advanced = later.minuteOfDay - earlier.minuteOfDay
        if (advanced < 0) return false
        val elapsedMinutes = (later.positionMillis - earlier.positionMillis).toDouble() / MILLIS_PER_MINUTE
        return advanced <= elapsedMinutes + SLACK_MINUTES
    }

    /**
     * Spread across the whole recording, not across the window the minute change is hunted in.
     *
     * Where the clock *is* does not change over the length of a file, so there is no reason to look
     * for it only in the first minute — and every reason not to, since that minute is where an app
     * launches, animates and covers its own status bar. A longer span also sharpens the test: over
     * three minutes a real clock advances about three minutes, which nothing else in the frame does.
     */
    private fun probePositions(spanMillis: Long): List<Long> =
        (0 until PROBES).map { probe -> spanMillis * probe / PROBES }

    private companion object {
        const val PROBES = 8
        const val MINIMUM_CHAIN = 4
        const val SLACK_MINUTES = 1.5
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
