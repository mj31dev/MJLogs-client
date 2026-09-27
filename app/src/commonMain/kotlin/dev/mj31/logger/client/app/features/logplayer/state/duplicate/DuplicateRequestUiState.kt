package dev.mj31.logger.client.app.features.logplayer.state.duplicate

import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * A file that was read, but repeats one already in the session, waiting for the user to say what to
 * do with it.
 *
 * [candidate] is the file already read, so answering costs no second read. [sharedRecordCount] is
 * how many of its records the open file already holds; for a copy it is all of them.
 */
data class DuplicateRequestUiState(
    val kind: DuplicateKind,
    val candidate: LogSource,
    val existingSourceId: String,
    val existingName: String,
    /** Where the open file lives: two files of one name are told apart only by their folders. */
    val existingPath: String,
    val sharedRecordCount: Int,
) {

    val choices: List<DuplicateChoice>
        get() = when (kind) {
            DuplicateKind.COPY -> listOf(DuplicateChoice.SKIP, DuplicateChoice.ADD_SEPARATELY)
            DuplicateKind.OVERLAP -> listOf(DuplicateChoice.MERGE, DuplicateChoice.ADD_SEPARATELY, DuplicateChoice.SKIP)
        }
}
