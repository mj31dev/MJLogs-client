package dev.mj31.logger.client.app.usecase.session

import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogFilter

/** Applies the active [LogFilter] to the merged entries. */
class FilterLogEntriesUseCase {

    operator fun invoke(entries: List<LogEntry>, filter: LogFilter): List<LogEntry> =
        if (!filter.isActive) entries else entries.filter { filter.matches(entry = it) }
}
