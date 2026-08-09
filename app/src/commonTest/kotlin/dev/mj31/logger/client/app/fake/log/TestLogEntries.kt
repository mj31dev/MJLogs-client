package dev.mj31.logger.client.app.fake.log

import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogEntry

/**
 * Builders for domain fixtures.
 *
 * Every factory exposes defaults so that a test only has to spell out the values it actually
 * asserts on, which keeps the intent of each test visible.
 */
object TestLogEntries {

    /** Arbitrary but fixed session origin; all relative timestamps are derived from it. */
    val BASE: Instant = Instant.parse("2024-05-01T10:00:00Z")

    /** Placeholder spec: the parser used by the tests is a fake, so the pattern content is irrelevant. */
    val SPEC: LogFormatSpec = LogFormatSpec(
        name = "test-format",
        linePattern = "(?<ts>\\d+)\\|(?<lvl>\\w+)\\|(?<tag>[^|]*)\\|(?<msg>.*)",
        timestampPattern = "epochMillis",
        fallbackLevel = LogLevel.INFO,
        origin = FormatOrigin.DETECTED,
    )

    /** Instant located [offsetMillis] after [BASE]; negative offsets move into the past. */
    fun at(offsetMillis: Long): Instant = BASE + offsetMillis.milliseconds

    fun entry(
        id: String = "src-1:1",
        sourceId: String = "src-1",
        lineNumber: Int = 1,
        timestamp: Instant = BASE,
        level: LogLevel = LogLevel.INFO,
        tag: String = "Network",
        message: String = "Connected",
        rawLine: String = "raw line",
    ): LogEntry = LogEntry(
        id = id,
        sourceId = sourceId,
        lineNumber = lineNumber,
        timestamp = timestamp,
        level = level,
        tag = tag,
        message = message,
        rawLine = rawLine,
    )

    /** Entry identified by [id] and positioned [offsetMillis] after [BASE]. */
    fun entryAt(
        offsetMillis: Long,
        id: String = "src-1:1",
        sourceId: String = "src-1",
        level: LogLevel = LogLevel.INFO,
        tag: String = "Network",
        message: String = "Connected",
    ): LogEntry = entry(
        id = id,
        sourceId = sourceId,
        timestamp = at(offsetMillis = offsetMillis),
        level = level,
        tag = tag,
        message = message,
    )

    /** [count] entries spaced [stepMillis] apart, ordered chronologically. */
    fun sequence(
        count: Int,
        stepMillis: Long = 1_000L,
        sourceId: String = "src-1",
        startOffsetMillis: Long = 0L,
    ): List<LogEntry> = List(size = count) { index ->
        entry(
            id = "$sourceId:${index + 1}",
            sourceId = sourceId,
            lineNumber = index + 1,
            timestamp = at(offsetMillis = startOffsetMillis + index * stepMillis),
            message = "Message $index",
        )
    }

    fun source(
        id: String = "src-1",
        name: String = "app.txt",
        path: String = "/logs/app.txt",
        format: LogFormatSpec = SPEC,
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
