package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.data.format.preview.RegexLogFormatPreviewer
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState

/** Two records with distinct messages, so a node can be addressed by its text alone. */
internal val uiEntries: List<LogEntry> = listOf(
    LogPlayerFixtures.entry(
        id = "e1",
        lineNumber = 1,
        offsetMillis = 0L,
        level = LogLevel.INFO,
        tag = "Network",
        message = "connected to server",
    ),
    LogPlayerFixtures.entry(
        id = "e2",
        lineNumber = 2,
        offsetMillis = 10_000L,
        level = LogLevel.ERROR,
        tag = "Storage",
        message = "write failed",
    ),
)

internal val uiSources: List<LogSourceUi> = listOf(
    LogSourceUi(
        id = LogPlayerFixtures.FIRST_SOURCE_ID,
        name = "app.txt",
        formatName = "ISO-8601",
        entryCount = 2,
        skippedLineCount = 0,
        isSelected = true,
    ),
)

/** Session with both records visible and nothing filtered out. */
internal fun loadedState(): LogPlayerState = LogPlayerState(
    sources = uiSources,
    entries = uiEntries,
    totalEntryCount = uiEntries.size,
)

/** Pending manual-format request, pre-filled with the layout inferred from [formatSampleLines]. */
internal fun pendingFormatRequest(): FormatRequestUiState {
    val draft = ManualFormatInput(
        timestampPattern = "dd.MM.yyyy_HH.mm.ss",
        structureTemplate = "<{any}>~{timestamp}~{tag}~{message}",
    )
    return FormatRequestUiState(
        path = "/logs/analytics.txt",
        fileName = "analytics.txt",
        sampleLines = formatSampleLines,
        reason = "No built-in log format matched any line of the sample.",
        timestampPattern = draft.timestampPattern,
        structureTemplate = draft.structureTemplate,
        preview = RegexLogFormatPreviewer().preview(input = draft, sampleLines = formatSampleLines),
        suggestion = draft,
    )
}

internal val formatSampleLines: List<String> = listOf(
    "<0000>~01.08.2026_10.23.45~ANALYTICS~event dispatched (0)",
    "<0001>~01.08.2026_10.23.46~ANALYTICS~event dispatched (1)",
)

/** Session long enough that a record in the middle can only be seen after scrolling. */
internal fun longSessionState(activeEntryId: String, followVideo: Boolean): LogPlayerState {
    val entries = (1..LONG_SESSION_SIZE).map { index ->
        LogPlayerFixtures.entry(
            id = "e$index",
            lineNumber = index,
            offsetMillis = index * 1_000L,
            message = "record number $index",
        )
    }
    return LogPlayerState(
        sources = uiSources,
        entries = entries,
        totalEntryCount = entries.size,
        activeEntryId = activeEntryId,
        followVideo = followVideo,
    )
}

private const val LONG_SESSION_SIZE = 200
