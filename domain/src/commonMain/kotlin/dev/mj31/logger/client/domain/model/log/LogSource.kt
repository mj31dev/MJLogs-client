package dev.mj31.logger.client.domain.model.log

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.TimeRange
import kotlinx.datetime.LocalDate

/**
 * One imported log file with the entries produced from it.
 *
 * Sources stay independent of each other so that a single file can be removed or re-parsed with a
 * different [format] without touching the rest of the session.
 *
 * [referenceDate] is the day the file's first record belongs to. A format whose timestamp carries a
 * full date ignores it; one that carries only a time of day cannot be placed without it, so it is
 * resolved once at import and travels with the source rather than being guessed again on each read.
 *
 * [preamble] is the text a file carries before its first record — a banner, the device it came
 * from, the moment logging started — kept verbatim, because it has no format of its own to be read
 * under. It is not persisted: like the entries, it is read again from the file.
 *
 * [zone] is the zone the file's clock was read in. The entries are true instants whatever it is; the
 * zone is what shows them the way the file wrote them.
 *
 * [extraParts] are further files merged into this one because they overlap it; [path], [name] and
 * [referenceDate] describe the first. Every part shares the [format] and the [zone].
 */
data class LogSource(
    val id: String,
    val name: String,
    val path: String,
    val format: LogFormatSpec,
    val entries: List<LogEntry>,
    val referenceDate: LocalDate,
    val skippedLineCount: Int = 0,
    val preamble: List<String> = emptyList(),
    val zone: SourceZone = SourceZone.UTC,
    val extraParts: List<LogSourcePart> = emptyList(),
) {

    /** Every file this source was read from, the first one first. */
    val paths: List<String>
        get() = listOf(path) + extraParts.map { it.path }

    val entryCount: Int
        get() = entries.size

    val timeRange: TimeRange?
        get() = TimeRange.of(instants = entries.map { it.timestamp })
}
