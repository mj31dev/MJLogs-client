package dev.mj31.logger.client.app.view.format

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test

class TimeFormattingTest {

    @Test
    fun `a video position under an hour is minutes, seconds and tenths`() {
        assertThat(formatVideoPosition(positionMillis = 0L)).isEqualTo("0:00.0")
        assertThat(formatVideoPosition(positionMillis = 1_500L)).isEqualTo("0:01.5")
        assertThat(formatVideoPosition(positionMillis = 61_000L)).isEqualTo("1:01.0")
        assertThat(formatVideoPosition(positionMillis = 599_900L)).isEqualTo("9:59.9")
    }

    @Test
    fun `an hour or more adds the hour and pads the minutes`() {
        assertThat(formatVideoPosition(positionMillis = 3_600_000L)).isEqualTo("1:00:00.0")
        assertThat(formatVideoPosition(positionMillis = 3_661_200L)).isEqualTo("1:01:01.2")
    }

    @Test
    fun `a negative position is clamped instead of rendering a broken value`() {
        assertThat(formatVideoPosition(positionMillis = -5_000L)).isEqualTo("0:00.0")
    }

    @Test
    fun `tenths are truncated, never rounded up into the next second`() {
        assertThat(formatVideoPosition(positionMillis = 1_999L)).isEqualTo("0:01.9")
    }

    @Test
    fun `a log time is rendered with milliseconds`() {
        val instant = Instant.parse("2024-01-15T10:23:45.123Z")

        assertThat(formatLogTime(instant = instant)).isEqualTo("10:23:45.123")
    }

    @Test
    fun `a log time pads every component`() {
        val instant = Instant.parse("2024-01-15T01:02:03.004Z")

        assertThat(formatLogTime(instant = instant)).isEqualTo("01:02:03.004")
    }

    @Test
    fun `timestamps are rendered in utc by default so they match the file`() {
        val instant = Instant.parse("2024-01-15T10:23:45Z")

        assertThat(formatLogTime(instant = instant)).isEqualTo("10:23:45.000")
        assertThat(formatLogTime(instant = instant, timeZone = TimeZone.of(zoneId = "Europe/Moscow")))
            .isEqualTo("13:23:45.000")
    }

    @Test
    fun `the full form prefixes the date`() {
        val instant = Instant.parse("2024-01-05T10:23:45.123Z")

        assertThat(formatLogDateTime(instant = instant)).isEqualTo("2024-01-05 10:23:45.123")
    }
}
