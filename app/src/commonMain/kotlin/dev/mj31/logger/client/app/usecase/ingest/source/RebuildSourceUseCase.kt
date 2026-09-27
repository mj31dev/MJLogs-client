package dev.mj31.logger.client.app.usecase.ingest.source

import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.usecase.ingest.date.buildPlaced
import dev.mj31.logger.client.app.usecase.ingest.duplicate.MergeSourcePartsUseCase
import dev.mj31.logger.client.app.usecase.ingest.rethrowCancellation
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.source.archive.LogFileExpander

/**
 * Reads a described source from its files again, merging its parts the way they were merged.
 *
 * No detection runs and no question is asked: how to read these files was settled when they were
 * first imported. The source keeps the identity it is described with, and its parts are merged in
 * their stored order, so the records come back with the ids they had.
 *
 * A part that no longer reads is left out rather than failing the source, the same leniency a whole
 * workspace gets for a file that went missing; the first file is the source, and without it there is
 * nothing to rebuild.
 */
class RebuildSourceUseCase(
    private val loader: LogSourceLoader,
    private val expander: LogFileExpander,
    private val resolveReferenceDate: ResolveReferenceDateUseCase,
    private val mergeParts: MergeSourcePartsUseCase,
) {

    suspend operator fun invoke(ref: LogSourceRef): LogSource? {
        val first = read(ref = ref, part = LogSourcePart(path = ref.path, name = ref.name, referenceDate = ref.referenceDate))
            ?: return null
        return ref.extraParts.fold(initial = first) { merged, part ->
            read(ref = ref, part = part)?.let { addition -> mergeParts(base = merged, addition = addition) } ?: merged
        }
    }

    private suspend fun read(ref: LogSourceRef, part: LogSourcePart): LogSource? = runCatching {
        val file = expander.expand(path = part.path).firstOrNull() ?: return@runCatching null
        // A description stored before the day was recorded has none; it goes through the very chain
        // a fresh import uses rather than being handed a constant this class would have to invent.
        resolveReferenceDate.buildPlaced(
            loader = loader,
            content = loader.read(path = file.path),
            spec = ref.format,
            displayName = part.name,
            modifiedAt = file.modifiedAt,
            statedDay = part.referenceDate,
            sourceId = ref.id,
        )?.first
    }.rethrowCancellation().getOrNull()
}
