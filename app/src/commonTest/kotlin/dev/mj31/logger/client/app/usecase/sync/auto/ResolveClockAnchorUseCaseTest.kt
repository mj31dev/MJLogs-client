package dev.mj31.logger.client.app.usecase.sync.auto

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.usecase.sync.auto.zone.ResolveClockAnchorUseCase
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReading
import kotlin.test.Test
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

/**
 * Turning `09:28` on a status bar into a moment.
 *
 * The clock and the log are taken to be the same clock, so the digits are never in doubt. What the
 * screen does not state is the day, and — on a twelve hour dial — which half of it; both are settled
 * by asking which reading puts the recording closest to the session that was loaded beside it.
 */
class ResolveClockAnchorUseCaseTest {

    private val resolve = ResolveClockAnchorUseCase()

    @Test
    fun `the digits on the screen are the digits in the log`() {
        val anchor = resolve(
            boundary = reading(hour = 9, minute = 28),
            logRange = range(from = "2026-08-08T09:20:00Z", to = "2026-08-08T09:40:00Z"),
            videoDurationMillis = MINUTE * 2,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-08-08T09:28:00Z"))
        assertThat(anchor.videoPositionMillis).isEqualTo(POSITION)
        assertThat(anchor.origin).isEqualTo(SyncOrigin.SCREEN_CLOCK)
        assertThat(anchor.accuracyMillis).isEqualTo(300L)
    }

    /**
     * A session that started the evening before and ran past midnight: the reading belongs to the day
     * it actually falls inside, not to the day the session began.
     */
    @Test
    fun `the day is the one that puts the recording in the session`() {
        val anchor = resolve(
            boundary = reading(hour = 9, minute = 28),
            logRange = range(from = "2026-08-07T23:50:00Z", to = "2026-08-08T10:00:00Z"),
            videoDurationMillis = MINUTE * 2,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-08-08T09:28:00Z"))
    }

    /**
     * The failure this was written for: an iPhone in twelve hour mode shows `2:39` and no meridiem,
     * beside a session that ran from 14:38 to 14:39. Taken at face value the recording lands in the
     * middle of the night, twelve hours from the logs it belongs to.
     */
    @Test
    fun `a twelve hour dial is read as the half of the day the logs are in`() {
        val anchor = resolve(
            boundary = reading(hour = 2, minute = 39, ambiguous = true),
            logRange = range(from = "2026-06-29T14:38:00Z", to = "2026-06-29T14:39:30Z"),
            videoDurationMillis = MINUTE * 2,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-06-29T14:39:00Z"))
    }

    /**
     * A recording started well before the logging: the clock changes minute in its opening seconds,
     * so that moment falls outside the session however the dial is read. What still separates the two
     * readings is that one of them puts the *recording* against the session and the other puts it
     * twelve hours away.
     */
    @Test
    fun `a recording that began before the logs is still read the right way round`() {
        val anchor = resolve(
            boundary = reading(hour = 2, minute = 31, ambiguous = true),
            logRange = range(from = "2026-06-29T14:38:00Z", to = "2026-06-29T14:39:30Z"),
            videoDurationMillis = MINUTE * 12,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-06-29T14:31:00Z"))
    }

    /**
     * Even a recording that never meets the session at all is read the way that comes nearest, which
     * is a far better answer than the half of the day the digits happen to spell.
     */
    @Test
    fun `a recording that misses the session entirely is read the way that comes closest`() {
        val anchor = resolve(
            boundary = reading(hour = 2, minute = 31, ambiguous = true),
            logRange = range(from = "2026-06-29T14:38:00Z", to = "2026-06-29T14:39:30Z"),
            videoDurationMillis = MINUTE,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-06-29T14:31:00Z"))
    }

    /** A padded hour states its half of the day, and must not be second-guessed against the logs. */
    @Test
    fun `an unambiguous reading is kept even when the other half would fit better`() {
        val anchor = resolve(
            boundary = reading(hour = 2, minute = 39),
            logRange = range(from = "2026-06-29T14:38:00Z", to = "2026-06-29T14:39:30Z"),
            videoDurationMillis = MINUTE * 2,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-06-29T02:39:00Z"))
    }

    /** When the literal reading already meets the session, the other half is never preferred. */
    @Test
    fun `the literal reading wins when it lands inside the logs`() {
        val anchor = resolve(
            boundary = reading(hour = 2, minute = 39, ambiguous = true),
            logRange = range(from = "2026-06-29T02:30:00Z", to = "2026-06-29T02:45:00Z"),
            videoDurationMillis = MINUTE * 2,
            accuracyMillis = 300L,
        )

        assertThat(anchor.logTimestamp).isEqualTo(Instant.parse(input = "2026-06-29T02:39:00Z"))
    }

    @Test
    fun `without any logs the reading is anchored on its own`() {
        val anchor = resolve(
            boundary = reading(hour = 9, minute = 28),
            logRange = null,
            videoDurationMillis = MINUTE,
            accuracyMillis = 300L,
        )

        assertThat(anchor.videoPositionMillis).isEqualTo(POSITION)
        assertThat(anchor.origin).isEqualTo(SyncOrigin.SCREEN_CLOCK)
    }

    private fun reading(hour: Int, minute: Int, ambiguous: Boolean = false) = ScreenClockReading(
        positionMillis = POSITION,
        time = LocalTime(hour = hour, minute = minute),
        isHalfDayAmbiguous = ambiguous,
    )

    private fun range(from: String, to: String) = TimeRange(
        start = Instant.parse(input = from),
        end = Instant.parse(input = to),
    )

    private companion object {
        const val POSITION = 17_000L
        const val MINUTE = 60_000L
    }
}
