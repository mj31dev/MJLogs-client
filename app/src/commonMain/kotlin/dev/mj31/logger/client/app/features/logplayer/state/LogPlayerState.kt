package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.SyncUiState
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState

/**
 * Single immutable snapshot rendered by the screen.
 *
 * Transient notifications are deliberately absent: they are [LogPlayerEffect.ShowMessage] events,
 * because replaying them on the next recomposition would be wrong.
 */
data class LogPlayerState(
    val sources: List<LogSourceUi> = emptyList(),
    val entries: List<LogEntry> = emptyList(),
    val totalEntryCount: Int = 0,
    val filter: LogFilter = LogFilter(),
    val timeWindowMillis: Long? = null,
    val selectedEntryId: String? = null,
    val activeEntryId: String? = null,
    val followVideo: Boolean = true,
    val video: VideoUiState = VideoUiState(),
    val sync: SyncUiState = SyncUiState(),
    val formatRequest: FormatRequestUiState? = null,
    val isImporting: Boolean = false,
) {

    val hasLogs: Boolean
        get() = totalEntryCount > 0

    val isFiltered: Boolean
        get() = entries.size != totalEntryCount
}
