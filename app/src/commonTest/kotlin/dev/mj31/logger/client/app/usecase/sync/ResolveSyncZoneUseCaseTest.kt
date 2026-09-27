package dev.mj31.logger.client.app.usecase.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlin.test.Test
import kotlinx.datetime.LocalDate

class ResolveSyncZoneUseCaseTest {

    private val resolve = ResolveSyncZoneUseCase()
    private val berlin = SourceZone(id = "Europe/Berlin", origin = ZoneOrigin.HEADER)
    private val tokyo = SourceZone(id = "Asia/Tokyo", origin = ZoneOrigin.CHOSEN)
    private val sources = listOf(source(id = "device", zone = berlin), source(id = "server", zone = tokyo))

    @Test
    fun `the screen is read in the zone of the file whose record is selected`() {
        assertThat(resolve(sources = sources, selectedSourceId = "server", chosenZoneId = null)).isEqualTo(tokyo)
    }

    @Test
    fun `with nothing selected the first file stands for the session`() {
        assertThat(resolve(sources = sources, selectedSourceId = null, chosenZoneId = null)).isEqualTo(berlin)
    }

    @Test
    fun `a zone the user chose for the screen outranks every file`() {
        val zone = resolve(sources = sources, selectedSourceId = "server", chosenZoneId = "UTC+05:00")

        assertThat(zone).isEqualTo(SourceZone(id = "UTC+05:00", origin = ZoneOrigin.CHOSEN))
    }

    @Test
    fun `an empty session is read in UTC`() {
        assertThat(resolve(sources = emptyList(), selectedSourceId = null, chosenZoneId = null)).isEqualTo(SourceZone.UTC)
    }

    private fun source(id: String, zone: SourceZone): LogSource = LogSource(
        id = id,
        name = "$id.log",
        path = "/logs/$id.log",
        format = TestLogEntries.SPEC,
        entries = emptyList(),
        referenceDate = LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 1),
        zone = zone,
    )
}
