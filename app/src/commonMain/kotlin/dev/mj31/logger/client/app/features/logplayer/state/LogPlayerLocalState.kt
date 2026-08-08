package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.domain.sync.screen.ClockRegion

/** Everything the view model tracks locally, i.e. what is not owned by a repository. */
data class LogPlayerLocalState(
    val filter: LogFilter = LogFilter(),
    val timeWindowMillis: Long? = null,
    val selectedEntryId: String? = null,
    val followVideo: Boolean = true,
    val isImporting: Boolean = false,
    val formatRequests: List<FormatRequestUiState> = emptyList(),
    val frameTime: String = "",
    val frameTimeError: Boolean = false,
    val isScanningClock: Boolean = false,
    val isSelectingClockRegion: Boolean = false,
    /** Where the clock was found, or where the user said it is; kept for the current screencast. */
    val clockRegion: ClockRegion? = null,
)
