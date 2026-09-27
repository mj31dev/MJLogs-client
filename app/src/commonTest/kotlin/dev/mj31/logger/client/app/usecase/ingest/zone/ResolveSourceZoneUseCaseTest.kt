package dev.mj31.logger.client.app.usecase.ingest.zone

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlin.test.Test

class ResolveSourceZoneUseCaseTest {

    private val resolve = ResolveSourceZoneUseCase()

    @Test
    fun `a zone the user chose outranks one the file names`() {
        val zone = resolve(spec = TestLogEntries.SPEC.withZoneId(zoneId = "Asia/Tokyo"), preamble = listOf("TZ: Europe/Berlin"))

        assertThat(zone).isEqualTo(SourceZone(id = "Asia/Tokyo", origin = ZoneOrigin.CHOSEN))
    }

    @Test
    fun `a zone announced under a name is taken from the header`() {
        listOf(
            "TZ: Europe/Berlin" to "Europe/Berlin",
            "timezone=UTC+3" to "UTC+03:00",
            "Time zone: GMT-05:30" to "UTC-05:30",
            "zone +0200" to "UTC+02:00",
        ).forEach { (line, expected) ->
            assertThat(headerZone(line)).isEqualTo(expected)
        }
    }

    @Test
    fun `a zone shown in passing is found too`() {
        assertThat(headerZone("Device: Pixel 8 (America/New_York)")).isEqualTo("America/New_York")
        assertThat(headerZone("Started at 10:00 UTC+3")).isEqualTo("UTC+03:00")
        assertThat(headerZone("Log started 2026-08-01 22:14:03 +03:00")).isEqualTo("UTC+03:00")
        assertThat(headerZone("Log started 2026-08-01T22:14:03.120Z")).isEqualTo("UTC")
    }

    @Test
    fun `a keyed zone outranks one seen in passing on an earlier line`() {
        val zone = resolve(
            spec = TestLogEntries.SPEC,
            preamble = listOf("Build 2026-08-01 10:00 +01:00", "TZ=Asia/Tokyo"),
        )

        assertThat(zone.id).isEqualTo("Asia/Tokyo")
    }

    @Test
    fun `something shaped like a region that is not one is ignored`() {
        val zone = resolve(spec = TestLogEntries.SPEC, preamble = listOf("Source: Android/Build 42", "Mars/Olympus"))

        assertThat(zone).isEqualTo(SourceZone.UTC)
    }

    @Test
    fun `nothing said means UTC by default`() {
        assertThat(resolve(spec = TestLogEntries.SPEC, preamble = emptyList())).isEqualTo(SourceZone.UTC)
    }

    private fun headerZone(line: String): String? =
        resolve(spec = TestLogEntries.SPEC, preamble = listOf(line)).takeIf { it.origin == ZoneOrigin.HEADER }?.id
}
