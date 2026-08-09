package dev.mj31.logger.client.domain.model.workspace

import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.sync.SyncAnchor

/**
 * Everything needed to reopen a workspace exactly where it was left.
 *
 * It describes a session; it does not contain one. Files are referenced by path and re-parsed on
 * restore, which is what keeps the store small and never stale.
 *
 * [packagePath] is set when the workspace came from a saved session file, and is what makes a later
 * save write back into that same file instead of asking for a new one.
 */
data class WorkspaceSnapshot(
    val logSources: List<LogSourceRef> = emptyList(),
    val video: VideoMedia? = null,
    val anchor: SyncAnchor? = null,
    val filter: LogFilter = LogFilter(),
    val timeWindowMillis: Long? = null,
    val followVideo: Boolean = true,
    val videoPositionMillis: Long = 0L,
    val packagePath: String? = null,
) {

    /** A workspace with neither logs nor a screencast is not worth restoring or offering to save. */
    val isEmpty: Boolean
        get() = logSources.isEmpty() && video == null

    companion object {
        val EMPTY: WorkspaceSnapshot = WorkspaceSnapshot()
    }
}
