package dev.mj31.logger.client.app.usecase.sync.auto.zone

import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReading
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Turns the moment a clock changed minute into an anchor.
 *
 * The clock on the screen and the times in the log are taken to be the same clock. A screen
 * recording accompanies the log its device produced, so `14:38` in the status bar and `14:38` in the
 * log pane are the same moment, and the anchor is built by putting those digits together with a day.
 *
 * No alternative *zone* is entertained. Enumerating the time zones in which the recording could also
 * make sense produces a list of moments that are all wrong but one, asks the user to recognise
 * which, and is only ever right about a device that was somewhere else than the log it is being read
 * beside — a case worth losing to keep the common one silent.
 *
 * Two things the screen genuinely does not state are settled here instead, and the session settles
 * both. The **day**, because a status bar shows none. And, on a twelve hour dial, **which half of
 * the day** — `2:39` beside a session that ran from `14:38` to `14:39` is the afternoon, and taking
 * the digits at face value would put the recording twelve hours away in the middle of the night.
 *
 * Neither is put to the user as a question. Each reading of the dial implies a recording, and the
 * one chosen is the reading whose recording comes closest to the loaded session — usually because it
 * lands squarely inside it, and otherwise because it is the nearest of the alternatives, which is
 * still the only sane answer when someone started recording before the logging began.
 */
class ResolveClockAnchorUseCase {

    operator fun invoke(
        boundary: ScreenClockReading,
        logRange: TimeRange?,
        videoDurationMillis: Long,
        accuracyMillis: Long,
    ): SyncAnchor {
        val readings = readingsOf(boundary = boundary)
        val fallback = anchorAt(
            instant = instantAt(
                date = logRange?.let { range -> dateOf(instant = range.start) } ?: dateOf(instant = EPOCH),
                time = readings.first(),
            ),
            boundary = boundary,
            accuracyMillis = accuracyMillis,
        )
        if (logRange == null) return fallback

        return candidates(readings = readings, range = logRange, boundary = boundary, accuracy = accuracyMillis)
            .minByOrNull { candidate ->
                distanceFrom(logRange = logRange, anchor = candidate, duration = videoDurationMillis)
            }
            ?: fallback
    }

    /**
     * How far the recording an anchor implies lies from the loaded session, in milliseconds.
     *
     * Zero means they meet, which is the answer that is being looked for; anything else is how badly
     * they miss. Asking only whether the anchored *moment* falls inside the logs is the obvious test
     * and it is wrong twice over. It fails on a recording started before the logging did — the clock
     * changes minute in the opening minute of the video, so that moment is outside the session
     * however it is read, and both readings of a twelve hour dial fail alike. And it has nothing to
     * say when neither reading fits, where being merely closer is still an answer.
     */
    private fun distanceFrom(logRange: TimeRange, anchor: SyncAnchor, duration: Long): Long {
        val start = anchor.videoStartInstant
        val end = if (duration > 0L) start + duration.milliseconds else start
        val short = logRange.start.toEpochMilliseconds() - end.toEpochMilliseconds()
        val late = start.toEpochMilliseconds() - logRange.end.toEpochMilliseconds()
        return maxOf(a = 0L, b = maxOf(a = short, b = late))
    }

    /**
     * The readings the dial allows, the literal one first.
     *
     * A twelve hour status bar that prints no `AM` states two moments at once. Where the digits are
     * unambiguous — a padded hour, or a meridiem beside them — there is only ever one reading.
     */
    private fun readingsOf(boundary: ScreenClockReading): List<LocalTime> =
        if (boundary.isHalfDayAmbiguous) {
            listOf(boundary.time, otherHalfOfDay(time = boundary.time))
        } else {
            listOf(boundary.time)
        }

    private fun otherHalfOfDay(time: LocalTime): LocalTime = LocalTime(
        hour = if (time.hour >= NOON) time.hour - NOON else time.hour + NOON,
        minute = time.minute,
    )

    /**
     * Every anchor the reading could produce, in the order they are preferred where nothing separates
     * them: the literal reading of the dial before its other half, and for each, the days the session
     * touches.
     */
    private fun candidates(
        readings: List<LocalTime>,
        range: TimeRange,
        boundary: ScreenClockReading,
        accuracy: Long,
    ): List<SyncAnchor> = readings.flatMap { time ->
        datesAround(range = range).map { day ->
            anchorAt(
                instant = instantAt(date = day, time = time),
                boundary = boundary,
                accuracyMillis = accuracy,
            )
        }
    }

    private fun anchorAt(instant: Instant, boundary: ScreenClockReading, accuracyMillis: Long): SyncAnchor =
        SyncAnchor(
            logTimestamp = instant,
            videoPositionMillis = boundary.positionMillis,
            origin = SyncOrigin.SCREEN_CLOCK,
            accuracyMillis = accuracyMillis,
        )

    /**
     * Read as UTC, which is how a log line without an offset of its own is read: both sides are wall
     * clock readings, and the zone they share cancels out of the comparison entirely.
     */
    private fun instantAt(date: LocalDate, time: LocalTime): Instant =
        LocalDateTime(date = date, time = time).toInstant(timeZone = TimeZone.UTC)

    /** The days the logs touch, plus one either side, which is where a reading near midnight lands. */
    private fun datesAround(range: TimeRange): List<LocalDate> {
        val first = dateOf(instant = range.start).minus(DatePeriod(days = 1))
        val last = dateOf(instant = range.end).plus(DatePeriod(days = 1))
        return generateSequence(seed = first) { day -> day.plus(DatePeriod(days = 1)) }
            .takeWhile { day -> day <= last }
            .toList()
    }

    private fun dateOf(instant: Instant): LocalDate = instant.toLocalDateTime(timeZone = TimeZone.UTC).date

    private companion object {
        const val NOON = 12
        val EPOCH = Instant.fromEpochMilliseconds(epochMilliseconds = 0L)
    }
}
