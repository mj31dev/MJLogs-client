package dev.mj31.logger.client.domain.source

/**
 * The single source of truth for the file types the workspace accepts.
 *
 * Native file dialogs only *suggest* a filter — some platforms ignore it, and a path can always
 * arrive from the command line — so every import is checked against this list as well.
 */
object SupportedFileTypes {

    val logExtensions: Set<String> = setOf(".txt", ".log")

    val videoExtensions: Set<String> = setOf(
        ".mp4",
        ".mov",
        ".m4v",
        ".mkv",
        ".avi",
        ".webm",
        ".mpeg",
        ".mpg",
        ".wmv",
    )

    fun extensionsOf(kind: MediaKind): Set<String> = when (kind) {
        MediaKind.LOG -> logExtensions
        MediaKind.VIDEO -> videoExtensions
    }

    fun kindOf(path: String): MediaKind? = MediaKind.entries.firstOrNull { kind -> accepts(kind = kind, path = path) }

    fun accepts(kind: MediaKind, path: String): Boolean =
        extensionsOf(kind = kind).any { path.endsWith(suffix = it, ignoreCase = true) }

    /** Human readable list used in dialogs and rejection messages, e.g. `.txt, .log`. */
    fun describe(kind: MediaKind): String = extensionsOf(kind = kind).joinToString(separator = ", ")

    fun rejectionMessage(kind: MediaKind, fileName: String): String =
        "$fileName is not a supported ${kind.name.lowercase()} file; expected one of: ${describe(kind = kind)}."
}
