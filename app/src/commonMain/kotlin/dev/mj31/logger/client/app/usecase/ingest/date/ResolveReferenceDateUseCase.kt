package dev.mj31.logger.client.app.usecase.ingest.date

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Decides which day the records of a file belong to when their timestamps do not say.
 *
 * This used to be the day the file happened to be opened, which quietly moved a log from last week
 * onto today and interleaved two rotated files as though they were one. The name is trusted over the
 * modification time because rotation writes the date into it deliberately, whereas copying a file
 * rewrites its modification time without anyone meaning to.
 *
 * A date the file writes into its own preamble outranks both: it is stated rather than inferred, and
 * it dates the first record, which is the day being asked for.
 *
 * A format whose timestamps carry their own date ignores the answer entirely.
 */
class ResolveReferenceDateUseCase(
    private val timeZone: TimeZone,
) {

    /** Returns the day and where it came from, or `null` when neither source could supply one. */
    operator fun invoke(
        fileName: String,
        modifiedAt: Instant?,
        preamble: List<String> = emptyList(),
    ): Pair<LocalDate, ReferenceDateOrigin>? {
        dateInPreamble(preamble = preamble)?.let { return it to ReferenceDateOrigin.HEADER }
        dateInName(fileName = fileName)?.let { return it to ReferenceDateOrigin.FILE_NAME }
        return modifiedAt
            ?.toLocalDateTime(timeZone = timeZone)
            ?.date
            ?.let { it to ReferenceDateOrigin.MODIFICATION_TIME }
    }

    /**
     * Reads `2024-08-01`, `2024_08_01` or `20240801` out of a file name.
     *
     * A bare eight digit run is only read as a date when it starts with a plausible year, so that an
     * identifier which merely happens to be eight digits long is not mistaken for one.
     */
    private fun dateInName(fileName: String): LocalDate? =
        SEPARATED_DATE.find(input = fileName)?.let { match ->
            dateOf(year = match.groupValues[1], month = match.groupValues[2], day = match.groupValues[3])
        } ?: COMPACT_DATE.find(input = fileName)?.value?.let { digits ->
            dateOf(
                year = digits.substring(startIndex = 0, endIndex = 4),
                month = digits.substring(startIndex = 4, endIndex = 6),
                day = digits.substring(startIndex = 6, endIndex = 8),
            )
        }

    /**
     * Reads the first date a preamble writes, year first (`2024-08-01`, `2024/08/01`) or day first
     * with dots (`01.08.2024`). A slash separated date written day or month first is left alone:
     * `03/04/2024` means two different days on two sides of the Atlantic.
     */
    private fun dateInPreamble(preamble: List<String>): LocalDate? = preamble.firstNotNullOfOrNull { line ->
        YEAR_FIRST_DATE.findAll(input = line).firstNotNullOfOrNull { match ->
            dateOf(year = match.groupValues[1], month = match.groupValues[3], day = match.groupValues[4])
        } ?: DAY_FIRST_DATE.findAll(input = line).firstNotNullOfOrNull { match ->
            dateOf(year = match.groupValues[3], month = match.groupValues[2], day = match.groupValues[1])
        }
    }

    private fun dateOf(year: String, month: String, day: String): LocalDate? = runCatching {
        LocalDate(year = year.toInt(), monthNumber = month.toInt(), dayOfMonth = day.toInt())
    }.getOrNull()

    private companion object {
        val SEPARATED_DATE = Regex(pattern = """(\d{4})[-_.](\d{2})[-_.](\d{2})""")
        val YEAR_FIRST_DATE = Regex(pattern = """(?<!\d)(\d{4})([-/.])(\d{2})\2(\d{2})(?!\d)""")
        val DAY_FIRST_DATE = Regex(pattern = """(?<!\d)(\d{2})\.(\d{2})\.(\d{4})(?!\d)""")
        val COMPACT_DATE = Regex(pattern = """\b(?:19|20)\d{6}\b""")
    }
}
