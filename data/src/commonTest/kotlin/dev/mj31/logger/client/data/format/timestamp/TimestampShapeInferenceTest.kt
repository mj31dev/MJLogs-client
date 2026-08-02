package dev.mj31.logger.client.data.format.timestamp

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class TimestampShapeInferenceTest {

    @Test
    fun `finds the timestamp anywhere in the line`() {
        val region = requireNotNull(TimestampShapeInference.findRegion(line = "<0042>~01.08.2026_10.23.45~END"))

        assertThat(region.text).isEqualTo("01.08.2026_10.23.45")
        assertThat(region.startIndex).isEqualTo(7)
        assertThat(region.hasOffset).isFalse()
    }

    @Test
    fun `keeps an explicit offset out of the shape but inside the region`() {
        val region = requireNotNull(TimestampShapeInference.findRegion(line = "2024-01-15T10:23:45.123+03:00 boot"))

        assertThat(region.text).isEqualTo("2024-01-15T10:23:45.123")
        assertThat(region.hasOffset).isTrue()
        assertThat(region.endIndex).isEqualTo("2024-01-15T10:23:45.123+03:00".length)
    }

    @Test
    fun `infers the common date layouts`() {
        assertThat(inferred(lines = listOf("2024-01-15 10:23:45.123"))).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS")
        assertThat(inferred(lines = listOf("2024/01/15 10:23:45"))).isEqualTo("yyyy/MM/dd HH:mm:ss")
        assertThat(inferred(lines = listOf("15.01.2024 10:23:45"))).isEqualTo("dd.MM.yyyy HH:mm:ss")
        assertThat(inferred(lines = listOf("2024-01-15T10:23:45.123+03:00"))).isEqualTo("yyyy-MM-ddTHH:mm:ss.SSSXXX")
    }

    @Test
    fun `infers a date-less logcat style timestamp`() {
        assertThat(inferred(lines = listOf("01-15 10:23:45.123 D/Tag: message"))).isEqualTo("MM-dd HH:mm:ss.SSS")
    }

    @Test
    fun `infers a time only timestamp`() {
        assertThat(inferred(lines = listOf("10:23:45.123 boot"))).isEqualTo("HH:mm:ss.SSS")
        assertThat(inferred(lines = listOf("10:23:45 boot"))).isEqualTo("HH:mm:ss")
    }

    @Test
    fun `infers epoch timestamps`() {
        assertThat(inferred(lines = listOf("1785555032085 boot"))).isEqualTo("epochMillis")
        assertThat(inferred(lines = listOf("1785555032 boot"))).isEqualTo("epochSeconds")
    }

    @Test
    fun `a value above twelve decides which component is the day`() {
        assertThat(inferred(lines = listOf("25/01/2024 10:23:45", "26/01/2024 10:23:45"))).isEqualTo("dd/MM/yyyy HH:mm:ss")
        assertThat(inferred(lines = listOf("01/25/2024 10:23:45", "01/26/2024 10:23:45"))).isEqualTo("MM/dd/yyyy HH:mm:ss")
    }

    @Test
    fun `microseconds are recognized as a longer fraction`() {
        assertThat(inferred(lines = listOf("2024-01-15 10:23:45.123456"))).isEqualTo("yyyy-MM-dd HH:mm:ss.SSSSSS")
    }

    @Test
    fun `lines that disagree on the shape yield nothing`() {
        assertThat(inferred(lines = listOf("2024-01-15 10:23:45", "10:23:45"))).isNull()
    }

    @Test
    fun `a line without any timestamp has no region`() {
        assertThat(TimestampShapeInference.findRegion(line = "just some free text")).isNull()
    }

    @Test
    fun `a shorter reading is available when the greedy one swallows a neighbour`() {
        val line = "01-15 10:23:45.123 1234 5678 D/Tag: message"

        val greedy = requireNotNull(TimestampShapeInference.findRegion(line = line))
        val narrow = requireNotNull(
            TimestampShapeInference.findRegion(line = line, maxGroups = TimestampShapeInference.MIN_GROUPS + 3),
        )

        assertThat(greedy.text.length).isGreaterThan(narrow.text.length)
        assertThat(narrow.text).isEqualTo("01-15 10:23:45.123")
    }

    private fun inferred(lines: List<String>): String? {
        val regions = lines.mapNotNull { TimestampShapeInference.findRegion(line = it) }
        return TimestampShapeInference.infer(regions = regions)
    }
}
