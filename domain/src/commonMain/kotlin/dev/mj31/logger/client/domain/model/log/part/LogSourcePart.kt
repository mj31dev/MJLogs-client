package dev.mj31.logger.client.domain.model.log.part

import kotlinx.datetime.LocalDate

/**
 * One more file a log source was assembled from, beyond the first.
 *
 * Two files are merged into one source when they are the same log caught at different moments — a
 * rotation that overlaps, or a copy taken later that holds the earlier one's tail — so that the
 * records they share are shown once. The source still names every file it came from, because the
 * files on disk remain the source of truth: each is read again on restore and merged again.
 *
 * [referenceDate] is the day this file's first record belongs to; `null` only in a description
 * stored before it was recorded.
 */
data class LogSourcePart(
    val path: String,
    val name: String,
    val referenceDate: LocalDate?,
)
