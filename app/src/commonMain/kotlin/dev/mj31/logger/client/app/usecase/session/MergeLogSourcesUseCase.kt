package dev.mj31.logger.client.app.usecase.session

import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Merges every imported file into one chronological session.
 *
 * The sort is stable, so records sharing a timestamp keep the import order of their files.
 */
class MergeLogSourcesUseCase {

    operator fun invoke(sources: List<LogSource>): LogSession = LogSession(
        sources = sources,
        entries = sources.flatMap { it.entries }.sortedBy { it.timestamp },
    )
}
