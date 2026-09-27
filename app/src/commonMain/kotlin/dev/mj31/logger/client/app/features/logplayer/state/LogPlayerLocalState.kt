package dev.mj31.logger.client.app.features.logplayer.state

import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.StartDayRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.UnsupportedImportUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.WorkspaceUiState
import dev.mj31.logger.client.domain.sync.screen.ClockRegion

/** Everything the view model tracks locally, i.e. what is not owned by a repository. */
data class LogPlayerLocalState(
    val filter: LogFilter = LogFilter(),
    val timeWindowMillis: Long? = null,
    val selectedEntryId: String? = null,
    val followVideo: Boolean = true,
    val isImporting: Boolean = false,
    val formatRequests: List<FormatRequestUiState> = emptyList(),
    val unsupportedImports: List<UnsupportedImportUiState> = emptyList(),
    val startDayRequests: List<StartDayRequestUiState> = emptyList(),
    val preambleSourceId: String? = null,
    val zoneRequest: ZoneTarget? = null,
    val duplicateRequests: List<DuplicateRequestUiState> = emptyList(),
    /** The zone the user said the screen's clock is in; `null` follows the logs. */
    val frameTimeZoneId: String? = null,
    /** File of the selected record, kept so that nothing has to search the session to find it. */
    val selectedSourceId: String? = null,
    val frameTime: String = "",
    val frameTimeError: Boolean = false,
    val isScanningClock: Boolean = false,
    val isSelectingClockRegion: Boolean = false,
    /** Where the clock was found, or where the user said it is; kept for the current screencast. */
    val clockRegion: ClockRegion? = null,
    /** The saved session file this workspace belongs to, and how far it is from what is on screen. */
    val workspace: WorkspaceUiState = WorkspaceUiState(),
)
