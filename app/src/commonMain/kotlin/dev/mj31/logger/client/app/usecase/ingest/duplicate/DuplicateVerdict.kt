package dev.mj31.logger.client.app.usecase.ingest.duplicate

import dev.mj31.logger.client.domain.model.log.LogSource

/** How a file about to join the session relates to the ones already in it. */
sealed interface DuplicateVerdict {

    /** Nothing in the session resembles it. */
    data object Distinct : DuplicateVerdict

    /** The very file is already open; opening it twice would show every record twice. */
    data class SamePath(val existing: LogSource) : DuplicateVerdict

    /** Another path, but the same records, down to the last line: a copy. */
    data class SameContent(val existing: LogSource) : DuplicateVerdict

    /**
     * The same log caught at another moment: some records are in both, the rest in one only. It is
     * reported only when both are read the same way — a merge under two formats or two zones would
     * not be one log.
     */
    data class Overlap(val existing: LogSource, val sharedRecordCount: Int) : DuplicateVerdict
}
