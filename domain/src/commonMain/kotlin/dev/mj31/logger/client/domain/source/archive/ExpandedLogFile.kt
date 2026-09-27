package dev.mj31.logger.client.domain.source.archive

import kotlin.time.Instant

/**
 * One readable log file produced from a path the user chose.
 *
 * [path] always denotes an ordinary file on disk: an entry of an archive has been written out by the
 * time this exists. Everything downstream — reading, the workspace snapshot, saving a session — then
 * deals in plain paths and never learns that an archive was involved.
 *
 * [displayName] is the name the file had inside its container, which is what the user recognizes.
 *
 * [modifiedAt] is carried here because it is only available while the container is open, and it is
 * the second link of the chain that decides which day a log without dated records belongs to.
 */
data class ExpandedLogFile(
    val path: String,
    val displayName: String,
    val modifiedAt: Instant? = null,
)
