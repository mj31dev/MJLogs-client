package dev.mj31.logger.client.domain.model.workspace

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/**
 * A log file a workspace was assembled from, described rather than materialized.
 *
 * The parsed [dev.mj31.logger.client.domain.model.log.LogEntry] values are deliberately absent: the
 * file on disk is the source of truth and is read again when the workspace is restored. Storing the
 * entries would duplicate the whole log inside the store, and a file that grew since the last visit
 * would come back truncated to whatever was captured then.
 */
data class LogSourceRef(
    val id: String,
    val name: String,
    val path: String,
    val format: LogFormatSpec,
)
