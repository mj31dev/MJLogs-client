package dev.mj31.logger.client.domain.session

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot

/**
 * A saved session file that has been opened.
 *
 * The paths inside [snapshot] already point at the extracted copies rather than at whatever the
 * archive stores internally: playback needs a real file on disk, and no caller should have to know
 * whether the bytes came out of an archive or off the file system.
 */
data class SessionPackage(
    val path: String,
    val name: String,
    val snapshot: WorkspaceSnapshot,
)
