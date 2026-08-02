package dev.mj31.logger.client.domain.format.parse

import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/** Creates a [LogLineParser] for a spec; compilation of regexes happens once per file. */
interface LogLineParserFactory {

    /**
     * @param referenceDate date used to complete timestamps whose pattern omits the date part
     * (for example Android logcat `MM-dd HH:mm:ss.SSS` or plain `HH:mm:ss`).
     * @throws IllegalArgumentException when [spec] cannot be compiled.
     */
    fun create(spec: LogFormatSpec, referenceDate: LocalDate): LogLineParser
}
