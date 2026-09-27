package dev.mj31.logger.client.domain.model.workspace

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import kotlinx.datetime.LocalDate

/**
 * A log file a workspace was assembled from, described rather than materialized.
 *
 * The parsed [dev.mj31.logger.client.domain.model.log.LogEntry] values are deliberately absent: the
 * file on disk is the source of truth and is read again when the workspace is restored. Storing the
 * entries would duplicate the whole log inside the store, and a file that grew since the last visit
 * would come back truncated to whatever was captured then.
 *
 * [referenceDate] is the exception to that rule, and has to be stored: it was inferred once — from
 * the file name, from the modification time, or by asking — and inferring it again on restore would
 * move a log whose records carry no date onto whatever day it was reopened.
 *
 * It is null only for a workspace stored before the date was recorded at all. Such a row is resolved
 * on restore through the same chain a fresh import uses, which is a better answer than any constant
 * this type could invent for it.
 *
 * [extraParts] are the further files a merged source was assembled from, each read again and merged
 * again on restore.
 */
data class LogSourceRef(
    val id: String,
    val name: String,
    val path: String,
    val format: LogFormatSpec,
    val referenceDate: LocalDate? = null,
    val extraParts: List<LogSourcePart> = emptyList(),
)
