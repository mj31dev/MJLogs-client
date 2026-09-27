package dev.mj31.logger.client.app.usecase.ingest.duplicate

import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Tells whether a file is already in the session, as itself, as a copy, or in part.
 *
 * The path is decisive on its own and needs nothing read, which is why it can also be asked before
 * a file is opened at all. Sameness of content is judged on the records, not the bytes: two copies
 * that differ only in a trailing blank line are the same log, and the records are what the session
 * would show twice.
 */
class DetectDuplicateUseCase {

    /** The source that already reads [path], if any; answerable before the file is opened. */
    operator fun invoke(path: String, sources: List<LogSource>): LogSource? =
        sources.firstOrNull { source -> path in source.paths }

    operator fun invoke(candidate: LogSource, sources: List<LogSource>): DuplicateVerdict {
        sources.firstOrNull { source -> candidate.path in source.paths }
            ?.let { existing -> return DuplicateVerdict.SamePath(existing = existing) }
        sources.firstOrNull { source -> sameContent(candidate = candidate, existing = source) }
            ?.let { existing -> return DuplicateVerdict.SameContent(existing = existing) }
        return sources.asSequence()
            .filter { source -> source.format == candidate.format && source.zone.id == candidate.zone.id }
            .map { source -> source to sharedCount(candidate = candidate, existing = source) }
            .firstOrNull { (_, shared) -> shared > 0 }
            ?.let { (existing, shared) -> DuplicateVerdict.Overlap(existing = existing, sharedRecordCount = shared) }
            ?: DuplicateVerdict.Distinct
    }

    private fun sameContent(candidate: LogSource, existing: LogSource): Boolean =
        candidate.entries.isNotEmpty() &&
            candidate.entries.size == existing.entries.size &&
            candidate.preamble == existing.preamble &&
            candidate.entries.indices.all { index ->
                candidate.entries[index].recordKey() == existing.entries[index].recordKey()
            }

    private fun sharedCount(candidate: LogSource, existing: LogSource): Int {
        val keys = existing.entries.mapTo(destination = HashSet(existing.entries.size)) { it.recordKey() }
        return candidate.entries.count { it.recordKey() in keys }
    }
}
