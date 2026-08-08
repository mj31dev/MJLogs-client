package dev.mj31.logger.client.app.usecase.sync.auto.screen

import dev.mj31.logger.client.domain.source.video.VideoScan
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReading
import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime

/**
 * Finds the frame on which the clock changed minute.
 *
 * This is the whole reason to look at the picture at all. A clock read off a frame locates it only
 * to the minute, which is worse than the container's own metadata. The *change* is different: at the
 * frame the status bar first shows a new minute, the wall clock is that minute exactly, to the
 * millisecond — an anchor as sharp as one a human places by hand, and available without one.
 *
 * The search is a bisection, which is what makes it cheap. Within any window of a minute the clock
 * changes exactly once, so a window of [WINDOW_MILLIS] is guaranteed to contain a change; each
 * reading halves what is left, and a dozen of them are enough to land on a single frame. Sweeping
 * the same window frame by frame would be hundreds.
 *
 * Two things make it more than textbook bisection. The window is checked at both ends first, because
 * a recording shorter than a minute may hold no change at all and there is no point hunting for one
 * that is not there. And a probe that lands on an unreadable stretch — a screen transition, a light
 * dialog over a light bar — does not end the search: it steps aside, in widening fractions of the
 * bracket, until it finds a frame that can be read.
 */
class FindMinuteChangeUseCase(
    private val readClock: ReadClockUseCase,
) {

    suspend operator fun invoke(scan: VideoScan, region: ClockRegion): MinuteChange? {
        val end = minOf(a = WINDOW_MILLIS, b = scan.durationMillis)
        if (end <= 0L) return null

        val start = readNear(scan = scan, region = region, target = 0L, low = 0L, high = end) ?: return null
        val finish = readNear(scan = scan, region = region, target = end, low = start.first, high = end)
        return if (finish == null || start.second == finish.second) {
            null
        } else {
            bisect(scan = scan, region = region, low = start, high = finish)
        }
    }

    /**
     * Narrows `(low, high]` until the two ends are one step apart; `high` is then the first position
     * the new minute is visible at, which is the moment the clock turned over.
     */
    private suspend fun bisect(
        scan: VideoScan,
        region: ClockRegion,
        low: Probe,
        high: Probe,
    ): MinuteChange {
        var lower = low
        var upper = high
        while (upper.first - lower.first > PRECISION_MILLIS) {
            val middle = lower.first + (upper.first - lower.first) / 2
            val probe = readNear(scan = scan, region = region, target = middle, low = lower.first, high = upper.first)
                ?: break
            if (probe.second == lower.second) lower = probe else upper = probe
        }
        // A reading is a vote over frames either side of the position, so the answer is only ever as
        // sharp as that spread, however narrow the bracket became.
        return MinuteChange(
            reading = ScreenClockReading(
                positionMillis = upper.first,
                time = upper.second.time,
                isHalfDayAmbiguous = upper.second.isHalfDayAmbiguous,
            ),
            accuracyMillis = upper.first - lower.first + 2 * TIGHT_SPREAD_MILLIS,
        )
    }

    /**
     * Reads as close to [target] as the picture allows, staying strictly inside `(low, high)`.
     *
     * Stepping aside in fractions of the bracket rather than by a fixed nudge is what gets past a
     * transition that lasts seconds: a hundred milliseconds either way would still be inside it.
     */
    private suspend fun readNear(
        scan: VideoScan,
        region: ClockRegion,
        target: Long,
        low: Long,
        high: Long,
    ): Probe? {
        val span = (high - low).coerceAtLeast(minimumValue = 1L)
        val attempted = mutableSetOf<Long>()
        OFFSET_FRACTIONS.forEach { fraction ->
            val position = (target + (span * fraction).toLong())
                .coerceIn(minimumValue = low, maximumValue = high)
            if (attempted.add(element = position)) {
                val time = readClock(
                    scan = scan,
                    region = region,
                    positionMillis = position,
                    spreadMillis = TIGHT_SPREAD_MILLIS,
                )
                if (time != null) return position to time
            }
        }
        return null
    }

    companion object {
        /**
         * Two seconds more than a minute: a change is certain inside any full minute, and the margin
         * keeps it off the very edge of the window, where a bracket has nothing left to halve.
         */
        const val WINDOW_MILLIS = 62_000L

        private const val PRECISION_MILLIS = 200L

        /** Frames read this far apart still describe the same instant for the purposes of a vote. */
        private const val TIGHT_SPREAD_MILLIS = 50L

        private val OFFSET_FRACTIONS = listOf(0.0, 0.125, -0.125, 0.25, -0.25, 0.375, -0.375)
    }
}

/** A position that could be read, and what it read. */
private typealias Probe = Pair<Long, ScreenClockTime>
