package dev.mj31.logger.client.app.fake

import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.source.TextFileContent
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParser

/**
 * Fixtures shared by the presentation layer tests.
 *
 * The two log files interleave on purpose (`0s / 20s / 40s` against `10s / 30s`) so that a merged
 * session is only chronological when the merge really sorts across files.
 */
object LogPlayerFixtures {

    /** Arbitrary but fixed session origin; every relative timestamp is derived from it. */
    val BASE: Instant = Instant.parse("2024-05-01T10:00:00Z")

    const val FIRST_PATH: String = "/logs/app.txt"
    const val FIRST_NAME: String = "app.txt"
    const val FIRST_SOURCE_ID: String = "src-1"

    const val SECOND_PATH: String = "/logs/network.txt"
    const val SECOND_NAME: String = "network.txt"
    const val SECOND_SOURCE_ID: String = "src-2"

    const val UNPARSABLE_PATH: String = "/logs/custom.txt"
    const val UNPARSABLE_NAME: String = "custom.txt"

    const val MISSING_PATH: String = "/logs/missing.txt"

    /** Entry ids are derived by the assembler as `<sourceId>:<lineNumber>`. */
    const val FIRST_ENTRY_ID: String = "src-1:1"
    const val SECOND_ENTRY_ID: String = "src-2:1"
    const val THIRD_ENTRY_ID: String = "src-1:2"
    const val FOURTH_ENTRY_ID: String = "src-2:2"
    const val FIFTH_ENTRY_ID: String = "src-1:3"

    val FIRST_SPEC: LogFormatSpec = LogFormatSpec(
        name = "logcat",
        linePattern = "(?<ts>\\d+)\\|(?<lvl>\\w+)\\|(?<tag>[^|]*)\\|(?<msg>.*)",
        timestampPattern = "epochMillis",
        origin = FormatOrigin.DETECTED,
    )

    val SECOND_SPEC: LogFormatSpec = FIRST_SPEC.copy(name = "syslog")

    val MANUAL_SPEC: LogFormatSpec = FIRST_SPEC.copy(name = "manual", origin = FormatOrigin.USER_DEFINED)

    /** Instant located [offsetMillis] after [BASE]; negative offsets move into the past. */
    fun at(offsetMillis: Long): Instant = BASE + offsetMillis.milliseconds

    val firstFile: TextFileContent = TextFileContent(
        path = FIRST_PATH,
        name = FIRST_NAME,
        lines = listOf(
            line(offsetMillis = 0L, level = LogLevel.INFO, tag = "Network", message = "Connected"),
            line(offsetMillis = 20_000L, level = LogLevel.WARN, tag = "Network", message = "Retrying handshake"),
            line(offsetMillis = 40_000L, level = LogLevel.ERROR, tag = "Network", message = "Crash detected"),
        ),
    )

    val secondFile: TextFileContent = TextFileContent(
        path = SECOND_PATH,
        name = SECOND_NAME,
        lines = listOf(
            line(offsetMillis = 10_000L, level = LogLevel.DEBUG, tag = "Storage", message = "Cache warmed"),
            line(offsetMillis = 30_000L, level = LogLevel.INFO, tag = "Storage", message = "Flush done"),
        ),
    )

    /** File whose lines the scripted parser never recognizes, whatever format it is given. */
    val unparsableFile: TextFileContent = TextFileContent(
        path = UNPARSABLE_PATH,
        name = UNPARSABLE_NAME,
        lines = listOf("an entirely unfamiliar line", "another unfamiliar line"),
    )

    /** Merged, chronologically ordered ids of [firstFile] plus [secondFile]. */
    val mergedEntryIds: List<String> = listOf(
        FIRST_ENTRY_ID,
        SECOND_ENTRY_ID,
        THIRD_ENTRY_ID,
        FOURTH_ENTRY_ID,
        FIFTH_ENTRY_ID,
    )

    fun line(
        offsetMillis: Long,
        level: LogLevel = LogLevel.INFO,
        tag: String = "Network",
        message: String = "Connected",
    ): String = ScriptedLogLineParser.recordLine(
        timestamp = at(offsetMillis = offsetMillis),
        level = level,
        tag = tag,
        message = message,
    )

    fun entry(
        id: String = FIRST_ENTRY_ID,
        sourceId: String = FIRST_SOURCE_ID,
        lineNumber: Int = 1,
        offsetMillis: Long = 0L,
        level: LogLevel = LogLevel.INFO,
        tag: String = "Network",
        message: String = "Connected",
    ): LogEntry = LogEntry(
        id = id,
        sourceId = sourceId,
        lineNumber = lineNumber,
        timestamp = at(offsetMillis = offsetMillis),
        level = level,
        tag = tag,
        message = message,
        rawLine = "raw line",
    )

    fun source(
        id: String = FIRST_SOURCE_ID,
        name: String = FIRST_NAME,
        path: String = FIRST_PATH,
        format: LogFormatSpec = FIRST_SPEC,
        entries: List<LogEntry> = emptyList(),
        skippedLineCount: Int = 0,
    ): LogSource = LogSource(
        id = id,
        name = name,
        path = path,
        format = format,
        entries = entries,
        skippedLineCount = skippedLineCount,
    )
}
