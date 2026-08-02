package dev.mj31.logger.client.app.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/** The typed time is read as UTC, which is the zone the workspace prints log timestamps in. */
class ParseFrameTimeUseCaseTest {

    private val useCase = ParseFrameTimeUseCase()

    @Test
    fun `a full date and time is read as typed`() {
        val parsed = useCase(text = "2024-05-01 10:00:20.500", referenceDate = null)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20.500Z"))
    }

    @Test
    fun `the ISO separator is accepted as well`() {
        val parsed = useCase(text = "2024-05-01T10:00:20", referenceDate = null)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20Z"))
    }

    @Test
    fun `a comma separates the fraction just as a dot does`() {
        val parsed = useCase(text = "2024-05-01 10:00:20,250", referenceDate = null)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20.250Z"))
    }

    @Test
    fun `a time without a date belongs to the day of the session`() {
        val parsed = useCase(text = "10:00:20.750", referenceDate = SESSION_DATE)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20.750Z"))
    }

    @Test
    fun `seconds and milliseconds may be left out`() {
        val parsed = useCase(text = "10:00", referenceDate = SESSION_DATE)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:00Z"))
    }

    @Test
    fun `a single fraction digit means tenths of a second`() {
        val parsed = useCase(text = "10:00:20.5", referenceDate = SESSION_DATE)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20.500Z"))
    }

    @Test
    fun `a typed date wins over the date of the session`() {
        val parsed = useCase(text = "2024-05-02 10:00:20", referenceDate = SESSION_DATE)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-02T10:00:20Z"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        val parsed = useCase(text = "  10:00:20  ", referenceDate = SESSION_DATE)

        assertThat(parsed).isEqualTo(Instant.parse("2024-05-01T10:00:20Z"))
    }

    @Test
    fun `a time without a date and without a session cannot be placed`() {
        assertThat(useCase(text = "10:00:20", referenceDate = null)).isNull()
    }

    @Test
    fun `text that is not a time is refused`() {
        assertThat(useCase(text = "yesterday evening", referenceDate = null)).isNull()
    }

    @Test
    fun `an impossible time is refused rather than rolled over`() {
        assertThat(useCase(text = "25:61:00", referenceDate = SESSION_DATE)).isNull()
    }

    @Test
    fun `an impossible date is refused`() {
        assertThat(useCase(text = "2024-02-31 10:00:00", referenceDate = null)).isNull()
    }

    @Test
    fun `a partially typed time is refused instead of being guessed`() {
        assertThat(useCase(text = "10:", referenceDate = SESSION_DATE)).isNull()
    }

    @Test
    fun `an empty field yields nothing`() {
        assertThat(useCase(text = "", referenceDate = SESSION_DATE)).isNull()
    }

    private companion object {
        val SESSION_DATE: LocalDate = LocalDate.parse(input = "2024-05-01")
    }
}
