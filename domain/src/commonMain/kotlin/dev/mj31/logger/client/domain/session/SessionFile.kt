package dev.mj31.logger.client.domain.session

/**
 * The one shape a saved session comes in.
 *
 * There used to be two — one carrying copies of the logs and the screencast, one holding only paths
 * to them — and the light one was rewritten on every change so that it always matched the screen.
 * That made "saved" mean two different things depending on which file you had, and a file that
 * silently stopped working when a log was moved is a worse thing to hand someone than no file. What
 * is left is self-contained and written only when asked.
 */
object SessionFile {

    const val EXTENSION: String = ".mjclog"

    fun matches(path: String): Boolean = path.endsWith(suffix = EXTENSION, ignoreCase = true)

    /** Adds the extension unless [path] already carries it, so "case" and "case.mjclog" agree. */
    fun withExtension(path: String): String = if (matches(path = path)) path else path + EXTENSION

    /** File name without directories or extension, which is what a session is called on screen. */
    fun nameOf(path: String): String = path
        .substringAfterLast(delimiter = '/')
        .substringAfterLast(delimiter = '\\')
        .removeSuffix(suffix = EXTENSION)
}
