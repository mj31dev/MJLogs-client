package dev.mj31.logger.client.app.usecase.ingest.date

import dev.mj31.logger.client.domain.format.spec.TimestampPatternTokens
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Checks the day a file was placed on against the moment it was last written.
 *
 * Records that carry only a time of day are placed on a day chosen from outside, and the parser then
 * rolls the day forward whenever time appears to go backwards. A log running from 22:00 to 02:00 is
 * therefore read as spanning two days — but *which* two is not settled by the file: the same clock
 * readings fit the day the file was written and the day before it equally well.
 *
 * Where only one of the two fits, that one is taken and nobody is asked. A placement fits when it
 * puts no record after the moment the file was last written, since a log cannot contain lines newer
 * than the file holding them.
 */
class ResolveStartDayUseCase(
    private val timeZone: TimeZone,
) {

    /**
     * What to do with a source that was built on [assumedStart].
     *
     * @param modifiedAt when the file was last written; without it nothing can be checked.
     */
    operator fun invoke(
        source: LogSource,
        assumedStart: LocalDate,
        modifiedAt: Instant?,
    ): StartDayResolution {
        if (TimestampPatternTokens.carriesDate(pattern = source.format.timestampPattern)) {
            return StartDayResolution.Settled(day = assumedStart)
        }
        val range = source.timeRange ?: return StartDayResolution.Settled(day = assumedStart)
        if (modifiedAt == null || !crossesMidnight(source = source)) {
            return StartDayResolution.Settled(day = assumedStart)
        }

        val previousDay = assumedStart.minusOneDay()
        val fitsAssumed = fits(end = range.end, modifiedAt = modifiedAt)
        val fitsPrevious = fits(end = range.end - 1.days, modifiedAt = modifiedAt)

        return when {
            fitsAssumed && fitsPrevious -> StartDayResolution.Ambiguous(
                candidates = listOf(assumedStart, previousDay),
            )

            fitsPrevious -> StartDayResolution.Settled(day = previousDay)
            else -> StartDayResolution.Settled(day = assumedStart)
        }
    }

    /** The rollover already moved the tail forward, so the last record sits on a later day than the first. */
    private fun crossesMidnight(source: LogSource): Boolean {
        val range = source.timeRange ?: return false
        return dayOf(instant = range.start) != dayOf(instant = range.end)
    }

    /**
     * A file cannot hold lines written after the file itself, give or take the slack a file system
     * timestamp and a log clock can drift apart by.
     */
    private fun fits(end: Instant, modifiedAt: Instant): Boolean = end <= modifiedAt + TOLERANCE

    private fun dayOf(instant: Instant): LocalDate = instant.toLocalDateTime(timeZone = timeZone).date

    private fun LocalDate.minusOneDay(): LocalDate =
        LocalDate.fromEpochDays(epochDays = toEpochDays() - 1)

    private companion object {

        /** Enough for a clock skew or a delayed flush, far less than the twelve hours that decide a day. */
        val TOLERANCE = 30.minutes
    }
}
