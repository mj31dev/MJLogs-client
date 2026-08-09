package dev.mj31.logger.client.app.usecase.workspace

/**
 * What came back when a stored workspace was reopened.
 *
 * A file that has moved or been deleted since it was imported is reported rather than swallowed:
 * silently opening four of five logs looks identical to opening all of them, and the missing one is
 * exactly what the user would go looking for later.
 */
data class WorkspaceRestoreResult(
    val restoredSourceCount: Int,
    val missingFileNames: List<String>,
) {

    val hasMissingFiles: Boolean
        get() = missingFileNames.isNotEmpty()

    companion object {
        val NOTHING: WorkspaceRestoreResult = WorkspaceRestoreResult(
            restoredSourceCount = 0,
            missingFileNames = emptyList(),
        )
    }
}
