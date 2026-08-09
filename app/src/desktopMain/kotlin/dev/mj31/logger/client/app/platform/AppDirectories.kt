package dev.mj31.logger.client.app.platform

import java.io.File

/**
 * Where the application keeps what belongs to it rather than to the user.
 *
 * Each platform has one answer to this and it is not the same answer, so the choice is made once
 * here instead of at every call site. The store goes where a backup tool will pick it up; the
 * unpacked copies of a session go where a cleaner is allowed to delete them.
 */
object AppDirectories {

    private const val FOLDER_NAME = "MJLogs"

    private val home: File get() = File(System.getProperty("user.home").orEmpty())

    private val isMac: Boolean get() = osName.contains(other = "mac")

    private val isWindows: Boolean get() = osName.contains(other = "win")

    private val osName: String get() = System.getProperty("os.name").orEmpty().lowercase()

    /** Durable: the workspace last open, and the session files that have been visited. */
    fun data(): File = when {
        isMac -> File(home, "Library/Application Support/$FOLDER_NAME")
        isWindows -> File(environment(name = "APPDATA") ?: home.path, FOLDER_NAME)
        else -> File(environment(name = "XDG_DATA_HOME") ?: File(home, ".local/share").path, FOLDER_NAME)
    }.apply { mkdirs() }

    /**
     * Disposable: what a full session package was unpacked into.
     *
     * Deleting it costs the next open of that package an unpack and nothing else, which is exactly
     * the bargain a cache directory is for.
     */
    fun cache(): File = when {
        isMac -> File(home, "Library/Caches/$FOLDER_NAME")
        isWindows -> File(environment(name = "LOCALAPPDATA") ?: home.path, "$FOLDER_NAME/cache")
        else -> File(environment(name = "XDG_CACHE_HOME") ?: File(home, ".cache").path, FOLDER_NAME)
    }.apply { mkdirs() }

    /** The application's own store. */
    fun databaseFile(): File = File(data(), "mjlogs.db")

    private fun environment(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
}
