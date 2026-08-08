package dev.mj31.logger.client.app.fake.video

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader
import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime
import kotlinx.datetime.LocalTime

/**
 * A clock whose every misbehaviour is scripted.
 *
 * A real recognizer fails in two ways that matter, and both are reproduced here on demand: a stretch
 * of the recording where nothing can be read at all, and a stretch where something plausible but
 * wrong is read, consistently, frame after frame. Those are the cases the search has to survive, and
 * against a real recording they cannot be arranged — they merely happen, somewhere, sometimes.
 *
 * The time shown at a position is derived from it, so the clock ticks exactly as a clock does.
 */
class ScriptedClockReader(
    private val startMillisOfDay: Long,
    private val unreadable: LongRange? = null,
    private val misreadingIn: LongRange? = null,
    private val misreading: LocalTime? = null,
    private val isHalfDayAmbiguous: Boolean = false,
    override val isAvailable: Boolean = true,
) : ScreenClockReader {

    override fun read(frame: VideoFrame, region: ClockRegion): ScreenClockTime? {
        val position = frame.sequence
        if (unreadable?.contains(value = position) == true) return null
        if (misreadingIn?.contains(value = position) == true) {
            return misreading?.let { time -> ScreenClockTime(time = time, isHalfDayAmbiguous = false) }
        }

        val millisOfDay = startMillisOfDay + position
        val minuteOfDay = (millisOfDay / MILLIS_PER_MINUTE) % MINUTES_PER_DAY
        return ScreenClockTime(
            time = LocalTime(
                hour = (minuteOfDay / MINUTES_PER_HOUR).toInt(),
                minute = (minuteOfDay % MINUTES_PER_HOUR).toInt(),
            ),
            isHalfDayAmbiguous = isHalfDayAmbiguous,
        )
    }

    override fun release() = Unit

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 1_440L
    }
}
