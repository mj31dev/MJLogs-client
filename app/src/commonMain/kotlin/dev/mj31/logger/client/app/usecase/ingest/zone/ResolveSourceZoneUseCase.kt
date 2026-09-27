package dev.mj31.logger.client.app.usecase.ingest.zone

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin

/**
 * Decides which zone the clock of a file runs in, when its timestamps do not say.
 *
 * A zone the user chose for the file wins. Failing that, a zone the file names before its first
 * record is taken — `TZ: Europe/Berlin`, `Started 2024-08-01 22:14:03 +03:00`, `(UTC+3)` — and
 * failing that, UTC, which is what every file used to be read in. An offset written into the
 * timestamps themselves outranks all three, but that is found while the lines are read rather than
 * decided here.
 */
class ResolveSourceZoneUseCase {

    operator fun invoke(spec: LogFormatSpec, preamble: List<String>): SourceZone {
        spec.zoneId?.let { chosen -> return SourceZone(id = chosen, origin = ZoneOrigin.CHOSEN) }
        return zoneInPreamble(preamble = preamble)?.let { id -> SourceZone(id = id, origin = ZoneOrigin.HEADER) }
            ?: SourceZone.UTC
    }

    /**
     * A zone announced under a name counts first, wherever it sits; then the first line that shows
     * one in passing. Every candidate is checked by opening it, so a word that merely looks like
     * `Area/Place` — a build path, a logger name — is not taken for a zone.
     */
    private fun zoneInPreamble(preamble: List<String>): String? =
        preamble.firstNotNullOfOrNull { line -> KEYED.find(input = line)?.let { SourceZone.idOf(text = it.groupValues[1]) } }
            ?: preamble.firstNotNullOfOrNull { line -> inPassing(line = line) }

    private fun inPassing(line: String): String? =
        REGION.findAll(input = line).firstNotNullOfOrNull { SourceZone.idOf(text = it.value) }
            ?: NAMED_OFFSET.find(input = line)?.let { SourceZone.idOf(text = it.value) }
            ?: OFFSET_AFTER_TIME.find(input = line)?.let { SourceZone.idOf(text = it.groupValues[1]) }

    private companion object {
        const val OFFSET = """[+-]\s*\d{1,2}(?::?\d{2})?"""

        val KEYED = Regex(
            pattern = """(?i)\b(?:time\s*zone|timezone|tz|zone)\b\s*[:=]?\s*""" +
                """((?:UTC|GMT)\s*$OFFSET|$OFFSET|[A-Za-z]+(?:/[A-Za-z_+\-]+)*)""",
        )
        val REGION = Regex(pattern = """\b[A-Z][A-Za-z_]+(?:/[A-Z][A-Za-z_\-]+)+\b""")
        val NAMED_OFFSET = Regex(pattern = """(?i)\b(?:UTC|GMT)\s*$OFFSET""")
        val OFFSET_AFTER_TIME = Regex(
            pattern = """\d{1,2}:\d{2}(?::\d{2}(?:[.,]\d+)?)?\s?(Z|[+-]\d{2}:?\d{2})(?![\d:])""",
        )
    }
}
