package dev.mj31.logger.client.app.usecase.ingest.duplicate

import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart

/**
 * Joins a further file onto a source, keeping each shared record once.
 *
 * Records are matched as a multiset rather than a set: a log that genuinely wrote the same line
 * twice in one millisecond keeps both, and only as many of the addition's copies are dropped as the
 * base already holds. What the base holds is kept as it is, so an anchor or a selection on one of its
 * records survives the merge.
 *
 * The addition's records take the base's identity and an id that names the part they came from, so
 * reading the same files again in the same order gives back the same ids — which is what lets a
 * restored anchor find its record.
 */
class MergeSourcePartsUseCase {

    operator fun invoke(base: LogSource, addition: LogSource): LogSource {
        val part = base.extraParts.size + 1
        val remaining = HashMap<RecordKey, Int>(base.entries.size)
        base.entries.forEach { entry -> remaining.merge(entry.recordKey(), 1, Int::plus) }

        val added = addition.entries.filter { entry -> !consume(remaining = remaining, key = entry.recordKey()) }
            .map { entry -> entry.inPart(sourceId = base.id, part = part) }

        return base.copy(
            entries = (base.entries + added).sortedBy { it.timestamp },
            skippedLineCount = base.skippedLineCount + addition.skippedLineCount,
            extraParts = base.extraParts + LogSourcePart(
                path = addition.path,
                name = addition.name,
                referenceDate = addition.referenceDate,
            ),
        )
    }

    /** True when the base still had a copy of this record to account for it. */
    private fun consume(remaining: MutableMap<RecordKey, Int>, key: RecordKey): Boolean {
        val left = remaining[key] ?: return false
        if (left == 1) remaining.remove(key) else remaining[key] = left - 1
        return true
    }

    private fun LogEntry.inPart(sourceId: String, part: Int): LogEntry =
        copy(id = "$sourceId:$part:$lineNumber", sourceId = sourceId)
}
