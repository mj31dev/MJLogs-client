package dev.mj31.logger.client.app.platform

/** Port for the native file dialogs; the desktop implementation lives in `desktopMain`. */
interface FileChooser {

    /** Returns the absolute path of the chosen screencast, or `null` when the user cancelled. */
    fun chooseVideo(): String?

    /** Returns the absolute paths of the chosen `.txt` log files; empty when the user cancelled. */
    fun chooseLogFiles(): List<String>
}
