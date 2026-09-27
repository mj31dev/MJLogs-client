package dev.mj31.logger.client.app.usecase.ingest.date

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class ResolveReferenceDateUseCaseTest {

    private val resolve = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC)
    private val modifiedAt = Instant.parse(input = "2024-01-15T10:00:00Z")

    @Test
    fun `a date in the preamble outranks the name and the modification time`() {
        val resolved = resolve(
            fileName = "app.log.2023-11-04",
            modifiedAt = modifiedAt,
            preamble = listOf("=== MyApp ===", "Session started 2022-02-03 22:14:00"),
        )

        assertThat(resolved).isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3) to ReferenceDateOrigin.HEADER)
    }

    @Test
    fun `slashes and dots between year first parts are read alike`() {
        assertThat(resolve(fileName = "a", modifiedAt = null, preamble = listOf("on 2022/02/03"))?.first)
            .isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3))
        assertThat(resolve(fileName = "a", modifiedAt = null, preamble = listOf("on 2022.02.03"))?.first)
            .isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3))
    }

    @Test
    fun `a day first date with dots is read day first`() {
        val resolved = resolve(fileName = "a", modifiedAt = null, preamble = listOf("Started: 03.02.2022 10:00"))

        assertThat(resolved?.first).isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3))
    }

    @Test
    fun `an ambiguous slash date written day or month first is not taken`() {
        val resolved = resolve(fileName = "a", modifiedAt = modifiedAt, preamble = listOf("Started 03/04/2022"))

        assertThat(resolved?.second).isEqualTo(ReferenceDateOrigin.MODIFICATION_TIME)
    }

    @Test
    fun `mismatched separators or an impossible date are not a date`() {
        val resolved = resolve(
            fileName = "a",
            modifiedAt = modifiedAt,
            preamble = listOf("build 2022-02/03", "range 2022-13-45"),
        )

        assertThat(resolved?.second).isEqualTo(ReferenceDateOrigin.MODIFICATION_TIME)
    }

    @Test
    fun `digits inside a longer number are not a date`() {
        val resolved = resolve(fileName = "a", modifiedAt = modifiedAt, preamble = listOf("id 120220-02-031"))

        assertThat(resolved?.second).isEqualTo(ReferenceDateOrigin.MODIFICATION_TIME)
    }

    @Test
    fun `without a preamble the name still outranks the modification time`() {
        val resolved = resolve(fileName = "app.log.2023-11-04", modifiedAt = modifiedAt)

        assertThat(resolved).isEqualTo(LocalDate(year = 2023, monthNumber = 11, dayOfMonth = 4) to ReferenceDateOrigin.FILE_NAME)
    }

    @Test
    fun `nothing to go on yields no day`() {
        assertThat(resolve(fileName = "app.log", modifiedAt = null, preamble = listOf("hello"))).isNull()
    }
}
