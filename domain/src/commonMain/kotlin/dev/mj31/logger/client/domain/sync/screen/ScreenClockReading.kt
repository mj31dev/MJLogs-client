package dev.mj31.logger.client.domain.sync.screen

import kotlinx.datetime.LocalTime

/**
 * The clock a single frame shows, and where that frame is in the recording.
 *
 * A status bar states hours and minutes and nothing else, so [time] carries no seconds: on its own
 * a reading locates a frame no better than to the minute. What makes it exact is the *change* —
 * between two frames reading different minutes lies the instant the minute turned over, and that
 * instant is known to the millisecond.
 */
data class ScreenClockReading(
    val positionMillis: Long,
    val time: LocalTime,
    /** When true the same digits could equally be the other half of the day; see [ScreenClockTime]. */
    val isHalfDayAmbiguous: Boolean = false,
) {

    /** Minutes since midnight, which is the whole of what a status bar clock actually says. */
    val minuteOfDay: Int
        get() = time.hour * MINUTES_PER_HOUR + time.minute

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
