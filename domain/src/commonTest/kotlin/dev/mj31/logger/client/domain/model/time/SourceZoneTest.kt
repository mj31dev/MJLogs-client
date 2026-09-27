package dev.mj31.logger.client.domain.model.time

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class SourceZoneTest {

    @Test
    fun `every spelling of an offset comes back the same way`() {
        listOf("UTC+3", "utc+03:00", "GMT+0300", "+03", "+03:00", " UTC + 3 ").forEach { spelling ->
            assertThat(SourceZone.idOf(text = spelling)).isEqualTo("UTC+03:00")
        }
        assertThat(SourceZone.idOf(text = "GMT-05:30")).isEqualTo("UTC-05:30")
    }

    @Test
    fun `zero and the names of UTC are all UTC`() {
        listOf("UTC", "utc", "Z", "GMT", "+00:00", "UTC-0").forEach { spelling ->
            assertThat(SourceZone.idOf(text = spelling)).isEqualTo(SourceZone.UTC_ID)
        }
    }

    @Test
    fun `a region name is kept as it is`() {
        assertThat(SourceZone.idOf(text = "Europe/Berlin")).isEqualTo("Europe/Berlin")
    }

    @Test
    fun `text that names no zone is refused`() {
        listOf("", "  ", "Mars/Olympus", "UTC+25", "+03:75", "tomorrow").forEach { spelling ->
            assertThat(SourceZone.idOf(text = spelling)).isNull()
        }
    }

    @Test
    fun `offsets in seconds are spelled with sign hours and minutes`() {
        assertThat(SourceZone.offsetId(seconds = 19_800)).isEqualTo("UTC+05:30")
        assertThat(SourceZone.offsetId(seconds = -3_600)).isEqualTo("UTC-01:00")
        assertThat(SourceZone.offsetId(seconds = 0)).isEqualTo("UTC")
    }

    @Test
    fun `every id it hands out opens as a zone`() {
        listOf("UTC+05:30", "UTC", "Europe/Berlin").forEach { id ->
            assertThat(SourceZone(id = id, origin = ZoneOrigin.CHOSEN).timeZone).isNotNull()
        }
    }
}
