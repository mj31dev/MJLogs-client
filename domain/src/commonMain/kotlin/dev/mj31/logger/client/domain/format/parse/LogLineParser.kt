package dev.mj31.logger.client.domain.format.parse

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/**
 * Parses lines of a single file with a pre-compiled [LogFormatSpec].
 *
 * Implementations are stateful only with respect to derived date information (see
 * [LogLineParserFactory.create]) and must be cheap to call per line.
 */
interface LogLineParser {
    fun parse(line: String): ParsedLine
}
