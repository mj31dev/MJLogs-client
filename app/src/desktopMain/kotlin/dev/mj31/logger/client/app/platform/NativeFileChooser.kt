package dev.mj31.logger.client.app.platform

import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * The platform's own file dialog: `java.awt.FileDialog` maps to the real Finder panel on macOS and
 * to the Explorer dialog on Windows.
 *
 * How far the type filter goes is up to the platform. macOS asks the [FilenameFilter] for every
 * entry and disables the ones it rejects; Windows ignores the callback and needs the glob in
 * [FileDialog.setFile] instead. Neither guarantee is strong enough on its own, so the selection is
 * validated once more before it leaves this class.
 */
class NativeFileChooser : FileChooser {

    override fun chooseVideo(): String? = open(
        title = "Select a screencast",
        multiple = false,
        kind = MediaKind.VIDEO,
    ).firstOrNull()

    override fun chooseLogFiles(): List<String> = open(
        title = "Select log files",
        multiple = true,
        kind = MediaKind.LOG,
    )

    private fun open(title: String, multiple: Boolean, kind: MediaKind): List<String> {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isMultipleMode = multiple
        dialog.filenameFilter = filenameFilterFor(kind = kind)
        if (isWindows) dialog.file = globFor(kind = kind)
        dialog.isVisible = true

        return dialog.files.orEmpty()
            .map(File::getAbsolutePath)
            .filter { SupportedFileTypes.accepts(kind = kind, path = it) }
    }

    private val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith(prefix = "Windows", ignoreCase = true)
}

/** Callback macOS consults for every entry of the panel. */
internal fun filenameFilterFor(kind: MediaKind): FilenameFilter =
    FilenameFilter { _, name -> SupportedFileTypes.accepts(kind = kind, path = name) }

/** Pattern Windows needs, since it never calls the filter: `*.txt;*.log`. */
internal fun globFor(kind: MediaKind): String =
    SupportedFileTypes.extensionsOf(kind = kind).joinToString(separator = ";") { "*$it" }
