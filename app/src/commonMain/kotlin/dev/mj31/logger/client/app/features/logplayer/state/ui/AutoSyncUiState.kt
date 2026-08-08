package dev.mj31.logger.client.app.features.logplayer.state.ui

/**
 * Presentation model of the synchronization the application performs by itself.
 *
 * The manual anchor is unaffected by any of it: these are extra ways to arrive at the same anchor,
 * and every one of them is overridden the moment the user places one by hand.
 */
data class AutoSyncUiState(
    val isScanning: Boolean = false,
    val canRun: Boolean = false,
    /** Offered only once an anchor exists that is coarser than a frame — that is, from metadata. */
    val canRefine: Boolean = false,
    val isSelectingRegion: Boolean = false,
)
