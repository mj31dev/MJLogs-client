package dev.mj31.logger.client.data.session

import java.io.File
import java.security.MessageDigest

/**
 * What a session file contains and where.
 *
 * A package is an ordinary archive so that it can be inspected, mailed and backed up without this
 * application, and so that a future version can add entries without invalidating the ones before it.
 *
 * ```
 * manifest.properties   layout version
 * session.db            the workspace, in the schema of the application store
 * logs/<n>-<name>       copies of the log files
 * video/<name>          copy of the screencast
 * ```
 */
internal object SessionPackageLayout {

    const val MANIFEST_ENTRY: String = "manifest.properties"
    const val DATABASE_ENTRY: String = "session.db"
    const val LOGS_PREFIX: String = "logs/"
    const val VIDEO_PREFIX: String = "video/"

    const val KEY_FORMAT_VERSION: String = "formatVersion"

    /**
     * Version of the layout itself, not of the database schema inside it.
     *
     * The two move independently: adding an entry here does not change a table, and a migration
     * between database versions does not move a file.
     */
    const val FORMAT_VERSION: Int = 1

    /** Suffix of the file being written; it never becomes the target until the write succeeded. */
    const val PARTIAL_SUFFIX: String = ".part"

    fun logEntryName(index: Int, fileName: String): String = "$LOGS_PREFIX$index-${sanitize(name = fileName)}"

    fun videoEntryName(fileName: String): String = "$VIDEO_PREFIX${sanitize(name = fileName)}"

    fun isBundledEntry(path: String): Boolean = path.startsWith(prefix = LOGS_PREFIX) ||
        path.startsWith(prefix = VIDEO_PREFIX)

    /**
     * Keeps an entry name to a single path segment.
     *
     * A log file called `../../etc/passwd` is not a realistic accident, but an archive is a file
     * format others can write, and an extractor that trusts its entry names writes wherever it is
     * told to.
     */
    fun sanitize(name: String): String = name
        .substringAfterLast(delimiter = '/')
        .substringAfterLast(delimiter = '\\')
        .ifBlank { "file" }

    /**
     * Where the contents of [packageFile] are unpacked to.
     *
     * The key covers the path *and* what the file currently is. Keyed by path alone, a package that
     * was rewritten in place would be read back from the copies made before the rewrite — and since
     * a rewritten workspace is very often exactly the same number of bytes as the one it replaced,
     * no cheap check inside the folder would notice.
     */
    fun extractedDirectory(cacheDirectory: File, packageFile: File): File = File(
        cacheDirectory,
        digestOf(text = "${packageFile.absolutePath}:${packageFile.lastModified()}:${packageFile.length()}"),
    )

    private fun digestOf(text: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(text.encodeToByteArray())
        .take(n = DIGEST_BYTES)
        .joinToString(separator = "") { byte -> byte.toUByte().toString(radix = HEX).padStart(2, '0') }

    private const val DIGEST_BYTES = 8
    private const val HEX = 16
}
