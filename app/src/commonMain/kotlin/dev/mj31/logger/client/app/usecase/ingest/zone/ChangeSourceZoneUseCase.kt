package dev.mj31.logger.client.app.usecase.ingest.zone

import dev.mj31.logger.client.app.usecase.ingest.source.RebuildSourceUseCase
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads one file of the session again in the zone the user chose for it.
 *
 * The entries are instants, so a different zone is a different set of them: nothing short of reading
 * the file again produces them. The source keeps its identity and its day, so a filter by file and an
 * anchor on one of its records still point where they did. A source merged from several files is read
 * again from all of them.
 *
 * `null` gives the choice back to the file — its preamble, or UTC.
 */
class ChangeSourceZoneUseCase(
    private val rebuildSource: RebuildSourceUseCase,
    private val sessionRepository: LogSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    /** Returns the source as read in its new zone, or `null` when the file could no longer be read. */
    suspend operator fun invoke(sourceId: String, zoneId: String?): LogSource? = withContext(context = dispatcher) {
        val current = sessionRepository.sources.value.firstOrNull { it.id == sourceId } ?: return@withContext null
        val rebuilt = rebuildSource(
            ref = LogSourceRef(
                id = current.id,
                name = current.name,
                path = current.path,
                format = current.format.withZoneId(zoneId = zoneId),
                referenceDate = current.referenceDate,
                extraParts = current.extraParts,
            ),
        ) ?: return@withContext null
        sessionRepository.replaceSource(source = rebuilt)
        rebuilt
    }
}
