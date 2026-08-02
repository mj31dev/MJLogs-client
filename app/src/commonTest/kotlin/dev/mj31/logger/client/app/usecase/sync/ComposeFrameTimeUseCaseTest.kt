package dev.mj31.logger.client.app.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlin.test.Test

/**
 * The picker only reaches minutes, so what it writes back has to keep the finer part the user typed.
 */
class ComposeFrameTimeUseCaseTest {

    private val useCase = ComposeFrameTimeUseCase(parseFrameTime = ParseFrameTimeUseCase())

    @Test
    fun `an empty field becomes the picked date and time`() {
        val text = useCase(dateMillis = DAY_MILLIS, hour = 18, minute = 50, previousText = "")

        assertThat(text).isEqualTo("2026-06-29 18:50:00.000")
    }

    @Test
    fun `seconds and milliseconds already typed survive the picking`() {
        val text = useCase(
            dateMillis = DAY_MILLIS,
            hour = 18,
            minute = 50,
            previousText = "2026-06-29 12:00:07.267",
        )

        assertThat(text).isEqualTo("2026-06-29 18:50:07.267")
    }

    @Test
    fun `a time without a date keeps its seconds too`() {
        val text = useCase(dateMillis = DAY_MILLIS, hour = 9, minute = 5, previousText = "12:00:07")

        assertThat(text).isEqualTo("2026-06-29 09:05:07.000")
    }

    @Test
    fun `unreadable text is dropped instead of being carried over`() {
        val text = useCase(dateMillis = DAY_MILLIS, hour = 0, minute = 0, previousText = "at some point")

        assertThat(text).isEqualTo("2026-06-29 00:00:00.000")
    }

    @Test
    fun `the result is understood by the parser it was built for`() {
        val text = useCase(dateMillis = DAY_MILLIS, hour = 18, minute = 50, previousText = "00:00:07.267")

        assertThat(ParseFrameTimeUseCase()(text = text, referenceDate = null))
            .isEqualTo(Instant.parse("2026-06-29T18:50:07.267Z"))
    }

    private companion object {
        /** Midnight UTC of 2026-06-29, which is what a date picker reports for that day. */
        val DAY_MILLIS: Long = Instant.parse("2026-06-29T00:00:00Z").toEpochMilliseconds()
    }
}
