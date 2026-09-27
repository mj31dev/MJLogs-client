package dev.mj31.logger.client.domain.source

/**
 * The single source of truth for the file types the workspace accepts.
 *
 * Native file dialogs only *suggest* a filter — some platforms ignore it, and a path can always
 * arrive from the command line — so every import is checked against this list as well.
 *
 * The list is a filter, not a verdict: a log has no reserved extension, and a file this object
 * rejects can still be opened when the user insists. What it buys is that a screencast or a photo
 * never becomes a log by accident.
 */
object SupportedFileTypes {

    /** Extensions whose content is read as text, one record per line. */
    val logExtensions: Set<String> = setOf(
        ".txt",
        ".log",
        ".out",
        ".err",
        ".json",
        ".jsonl",
        ".ndjson",
        ".csv",
        ".tsv",
    )

    /** Containers holding one or more of the above; unpacked before anything reads them. */
    val archiveExtensions: Set<String> = setOf(".gz", ".zip")

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
        MediaKind.LOG -> logExtensions + archiveExtensions
        MediaKind.VIDEO -> videoExtensions
    }

    fun kindOf(path: String): MediaKind? = MediaKind.entries.firstOrNull { kind -> accepts(kind = kind, path = path) }

    fun accepts(kind: MediaKind, path: String): Boolean {
        val name = fileNameOf(path = path)
        if (extensionsOf(kind = kind).any { name.endsWith(suffix = it, ignoreCase = true) }) return true
        return kind == MediaKind.LOG && isRotated(name = name)
    }

    /** True when the path denotes a container rather than a readable log. */
    fun isArchive(path: String): Boolean {
        val name = fileNameOf(path = path)
        return archiveExtensions.any { name.endsWith(suffix = it, ignoreCase = true) }
    }

    /** Human readable list used in dialogs and rejection messages, e.g. `.txt, .log`. */
    fun describe(kind: MediaKind): String = extensionsOf(kind = kind).joinToString(separator = ", ")

    fun rejectionMessage(kind: MediaKind, fileName: String): String =
        "$fileName is not a supported ${kind.name.lowercase()} file; expected one of: ${describe(kind = kind)}."

    /**
     * Recognizes what log rotation leaves behind: `app.log.1`, `app.log.2024-08-01`, `app.log.1.gz`.
     *
     * Only a counter or a date is accepted as the trailing part, and never an arbitrary one. A rule
     * that merely looked for `.log.` anywhere in the name would claim `capture.log.mp4`, and a
     * screencast that is treated as a log is a worse failure than a rotated file that is not.
     */
    private fun isRotated(name: String): Boolean {
        val withoutArchive = archiveExtensions
            .firstOrNull { name.endsWith(suffix = it, ignoreCase = true) }
            ?.let { name.dropLast(n = it.length) }
            ?: name
        val rotation = ROTATION_SUFFIX.find(input = withoutArchive) ?: return false
        val base = withoutArchive.dropLast(n = rotation.value.length)
        return logExtensions.any { base.endsWith(suffix = it, ignoreCase = true) }
    }

    private fun fileNameOf(path: String): String =
        path.substringAfterLast(delimiter = '/').substringAfterLast(delimiter = '\\')

    /** A rotation counter (`.1`) or an ISO date (`.2024-08-01`) at the very end of the name. */
    private val ROTATION_SUFFIX = Regex(pattern = """\.(?:\d+|\d{4}-\d{2}-\d{2})$""")
}
