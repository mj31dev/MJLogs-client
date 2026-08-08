package dev.mj31.logger.client.data.sync.screen

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.datetime.LocalTime

/**
 * What the recognizer hands back, and what can be made of it.
 *
 * The strings here are not invented: they are the shapes a status bar crop actually produces — a
 * colon that came out as a full stop, a zero read as a letter, and above all the bare `2:39` that an
 * iPhone in twelve hour mode shows, with no `PM` anywhere on the screen to disambiguate it.
 */
class ClockTextParserTest {

    private val parser = ClockTextParser()

    @Test
    fun `a padded hour comes from a twenty four hour dial and settles the day`() {
        val parsed = parser.parse(text = "09:28")

        assertThat(parsed?.time).isEqualTo(LocalTime(hour = 9, minute = 28))
        assertThat(parsed?.isHalfDayAmbiguous).isFalse()
    }

    @Test
    fun `an hour past twelve can only be a twenty four hour dial`() {
        val parsed = parser.parse(text = "21:41")

        assertThat(parsed?.time).isEqualTo(LocalTime(hour = 21, minute = 41))
        assertThat(parsed?.isHalfDayAmbiguous).isFalse()
    }

    /**
     * The case that sent an afternoon recording into the middle of the night: a twelve hour dial does
     * not pad the hour, and iOS prints no meridiem beside it, so `2:39` is two moments.
     */
    @Test
    fun `a bare hour comes from a twelve hour dial and settles nothing`() {
        val parsed = parser.parse(text = "2:39")

        assertThat(parsed?.time).isEqualTo(LocalTime(hour = 2, minute = 39))
        assertThat(parsed?.isHalfDayAmbiguous).isTrue()
    }

    /** Ten, eleven and twelve are written the same way by either dial, so they stay open. */
    @Test
    fun `the hours both dials spell alike stay open`() {
        assertThat(parser.parse(text = "12:39")?.isHalfDayAmbiguous).isTrue()
        assertThat(parser.parse(text = "10:05")?.isHalfDayAmbiguous).isTrue()
    }

    @Test
    fun `a meridiem settles the day whatever the padding`() {
        val morning = parser.parse(text = "9:41 AM")
        val evening = parser.parse(text = "9:41 PM")

        assertThat(morning?.time).isEqualTo(LocalTime(hour = 9, minute = 41))
        assertThat(morning?.isHalfDayAmbiguous).isFalse()
        assertThat(evening?.time).isEqualTo(LocalTime(hour = 21, minute = 41))
        assertThat(evening?.isHalfDayAmbiguous).isFalse()
    }

    /** Midnight and noon are the two the twelve hour clock names the least obviously. */
    @Test
    fun `noon and midnight are read the way the dial means them`() {
        assertThat(parser.parse(text = "12:05 AM")?.time).isEqualTo(LocalTime(hour = 0, minute = 5))
        assertThat(parser.parse(text = "12:05 PM")?.time).isEqualTo(LocalTime(hour = 12, minute = 5))
    }

    @Test
    fun `a separator the recognizer mangled is still a separator`() {
        assertThat(parser.parse(text = "09.28")?.time).isEqualTo(LocalTime(hour = 9, minute = 28))
        assertThat(parser.parse(text = "09 28")?.time).isEqualTo(LocalTime(hour = 9, minute = 28))
    }

    @Test
    fun `letters the recognizer saw instead of digits are read back as digits`() {
        assertThat(parser.parse(text = "O9:2B")?.time).isEqualTo(LocalTime(hour = 9, minute = 28))
        assertThat(parser.parse(text = "l0:15")?.time).isEqualTo(LocalTime(hour = 10, minute = 15))
    }

    @Test
    fun `a crop with no time in it yields nothing`() {
        assertThat(parser.parse(text = "")).isNull()
        assertThat(parser.parse(text = "Settings")).isNull()
        assertThat(parser.parse(text = "5")).isNull()
    }

    @Test
    fun `an impossible reading is refused rather than clamped`() {
        assertThat(parser.parse(text = "29:15")).isNull()
        assertThat(parser.parse(text = "09:71")).isNull()
    }
}
