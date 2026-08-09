package dev.mj31.logger.client.app.features.logplayer.state.ui

/**
 * What the screen shows about the saved session the workspace belongs to.
 *
 * [hasUnsavedChanges] is the ordinary state of a workspace being worked on, not an exception: the
 * file carries copies of the logs and the screencast and is rewritten only when asked, so anything
 * done between two saves leaves it behind.
 */
data class WorkspaceUiState(
    val packagePath: String? = null,
    val packageName: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val save: PackageSaveUiState? = null,
) {

    val isSaving: Boolean
        get() = save != null

    val isBoundToPackage: Boolean
        get() = packagePath != null
}

/** Progress of a write long enough to need a bar and a way out of it. */
data class PackageSaveUiState(
    val fileName: String,
    val fraction: Float,
    val copiedBytes: Long,
    val totalBytes: Long,
)
