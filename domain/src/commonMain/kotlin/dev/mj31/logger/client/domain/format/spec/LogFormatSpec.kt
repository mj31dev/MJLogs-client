package dev.mj31.logger.client.domain.format.spec

import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * Complete description of how one line of a log file is decomposed.
 *
 * [linePattern] is a regular expression that must expose the named groups declared in
 * [LogFormatGroups]; only [LogFormatGroups.TIMESTAMP] is mandatory. [timestampPattern] describes
 * the timestamp layout using the token subset documented in [TimestampPatternTokens].
 */
data class LogFormatSpec(
    val name: String,
    val linePattern: String,
    val timestampPattern: String,
    val fallbackLevel: LogLevel = LogLevel.INFO,
    val utcOffsetMinutes: Int = 0,
    val origin: FormatOrigin = FormatOrigin.DETECTED,
)
