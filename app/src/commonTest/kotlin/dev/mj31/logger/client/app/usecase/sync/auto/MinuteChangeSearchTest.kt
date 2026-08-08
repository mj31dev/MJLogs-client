package dev.mj31.logger.client.app.usecase.sync.auto

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.video.ScriptedClockReader
import dev.mj31.logger.client.app.fake.video.ScriptedVideoScan
import dev.mj31.logger.client.app.usecase.sync.auto.screen.FindMinuteChangeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.ReadClockUseCase
import dev.mj31.logger.client.domain.sync.screen.ClockRegionPresets
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime

/**
 * The exact behaviour of the search for the frame a clock changed minute.
 *
 * A real recording proves the recognizer copes with a real picture; it cannot prove where the change
 * truly was, because nothing in the file says so. Here the clock is scripted, so the answer is known
 * to the millisecond and the search can be held to it — including when a stretch of the recording
 * reads nothing, and when a stretch reads something plausible and wrong.
 */
class MinuteChangeSearchTest {

    private val region = ClockRegionPresets.TOP

    @Test
    fun `the change is found within the precision the search reports`() = runTest {
        val change = search(startMillisOfDay = at(hour = 9, minute = 27, second = 43))

        assertThat(change).isNotNull()
        requireNotNull(change)
        assertThat(change.reading.time).isEqualTo(LocalTime(hour = 9, minute = 28))
        assertThat(change.reading.positionMillis)
            .isIn((EXPECTED_CHANGE - change.accuracyMillis)..(EXPECTED_CHANGE + change.accuracyMillis))
        assertThat(change.accuracyMillis).isAtMost(MAX_ACCURACY_MILLIS)
    }

    @Test
    fun `the minute before the change is one lower`() = runTest {
        val reader = ScriptedClockReader(startMillisOfDay = at(hour = 9, minute = 27, second = 43))
        val scan = ScriptedVideoScan(durationMillis = DURATION_MILLIS)
        val readClock = ReadClockUseCase(reader = reader)
        val change = requireNotNull(FindMinuteChangeUseCase(readClock = readClock)(scan = scan, region = region))

        val before = readClock(
            scan = scan,
            region = region,
            positionMillis = change.reading.positionMillis - BEFORE_MILLIS,
            spreadMillis = TIGHT_SPREAD_MILLIS,
        )

        assertThat(before?.time).isEqualTo(LocalTime(hour = 9, minute = 27))
    }

    @Test
    fun `a stretch that cannot be read does not stop the search`() = runTest {
        val change = search(
            startMillisOfDay = at(hour = 9, minute = 27, second = 43),
            unreadable = 5_000L..25_000L,
        )

        assertThat(change).isNotNull()
        assertThat(requireNotNull(change).reading.time).isEqualTo(LocalTime(hour = 9, minute = 28))
    }

    /**
     * A confidently wrong reading in the middle of the window is the failure a real recognizer
     * actually produces, and it must not drag the bisection away from the true change.
     */
    @Test
    fun `a plausible misreading in the middle does not move the answer`() = runTest {
        val change = search(
            startMillisOfDay = at(hour = 9, minute = 27, second = 43),
            misreadingIn = 30_000L..40_000L,
            misreading = LocalTime(hour = 1, minute = 9),
        )

        assertThat(change).isNotNull()
        assertThat(requireNotNull(change).reading.time).isEqualTo(LocalTime(hour = 9, minute = 28))
    }

    @Test
    fun `a recording that spans no change reports none`() = runTest {
        val change = search(
            startMillisOfDay = at(hour = 9, minute = 27, second = 5),
            durationMillis = 12_000L,
        )

        assertThat(change).isNull()
    }

    private suspend fun search(
        startMillisOfDay: Long,
        durationMillis: Long = DURATION_MILLIS,
        unreadable: LongRange? = null,
        misreadingIn: LongRange? = null,
        misreading: LocalTime? = null,
    ) = FindMinuteChangeUseCase(
        readClock = ReadClockUseCase(
            reader = ScriptedClockReader(
                startMillisOfDay = startMillisOfDay,
                unreadable = unreadable,
                misreadingIn = misreadingIn,
                misreading = misreading,
            ),
        ),
    )(scan = ScriptedVideoScan(durationMillis = durationMillis), region = region)

    private fun at(hour: Int, minute: Int, second: Int): Long =
        ((hour * MINUTES_PER_HOUR + minute) * SECONDS_PER_MINUTE + second) * MILLIS_PER_SECOND

    private companion object {
        const val MINUTES_PER_HOUR = 60L
        const val SECONDS_PER_MINUTE = 60L
        const val MILLIS_PER_SECOND = 1_000L

        const val DURATION_MILLIS = 76_000L

        /** The clock starts at 09:27:43, so 09:28:00 falls seventeen seconds in. */
        const val EXPECTED_CHANGE = 17_000L

        /** The bisection bracket plus the spread the vote at each end samples over. */
        const val MAX_ACCURACY_MILLIS = 300L
        const val BEFORE_MILLIS = 1_000L
        const val TIGHT_SPREAD_MILLIS = 50L
    }
}
