package dev.mj31.logger.client.app.platform


/** Port for the native file dialogs; the desktop implementation lives in `desktopMain`. */
interface FileChooser {

    /** Returns the absolute path of the chosen screencast, or `null` when the user cancelled. */
    fun chooseVideo(): String?

    /** Returns the absolute paths of the chosen `.txt` log files; empty when the user cancelled. */
    fun chooseLogFiles(): List<String>

    /** Returns the absolute path of the saved session to open, or `null` when the user cancelled. */
    fun chooseSessionFile(): String?

    /**
     * Asks where a session of [kind] should be written, suggesting [suggestedName].
     *
     * The returned path may lack the extension: the dialog lets the user type whatever they want,
     * and correcting it is a decision of the use case rather than of the dialog.
     */
    fun chooseSessionTarget(suggestedName: String): String?
}
