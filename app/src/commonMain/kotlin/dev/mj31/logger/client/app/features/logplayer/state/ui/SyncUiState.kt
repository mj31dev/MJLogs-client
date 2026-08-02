package dev.mj31.logger.client.app.features.logplayer.state.ui

import dev.mj31.logger.client.app.usecase.timeline.TimelineOverlap
import kotlinx.datetime.Instant

/** Presentation model of the manual synchronization state. */
data class SyncUiState(
    val isSynced: Boolean = false,
    val anchorEntryId: String? = null,
    val anchorVideoPositionMillis: Long = 0L,
    val logTimeAtPlayhead: Instant? = null,
    val overlap: TimelineOverlap? = null,
    val canSynchronize: Boolean = false,
    /** Wall clock time the user typed for the current frame, and whether it could be read. */
    val frameTime: String = "",
    val frameTimeError: Boolean = false,
    val canSynchronizeAtFrameTime: Boolean = false,
    /** Moment the picker opens on: the typed time when it is readable, else the session start. */
    val frameTimeDefault: Instant? = null,
)
