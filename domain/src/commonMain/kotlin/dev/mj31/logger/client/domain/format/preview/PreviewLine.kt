package dev.mj31.logger.client.domain.format.preview

import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * One sample line as the current format would read it.
 *
 * When [isRecord] is false the line does not start a record and would be appended to the previous
 * one, which is exactly what the parser does at import time.
 */
data class PreviewLine(
    val text: String,
    val spans: List<HighlightedSpan> = emptyList(),
    val isRecord: Boolean = false,
    val level: LogLevel? = null,
)
