package dev.mj31.logger.client.data.source.archive

import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import dev.mj31.logger.client.domain.source.archive.ExpandedLogFile
import dev.mj31.logger.client.domain.source.archive.LogFileExpander
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Unpacks a chosen path into readable files inside [cacheDirectory].
 *
 * Unpacking happens once, at import, rather than on every read. An entry of an archive cannot grow
 * the way a live log file can, so nothing is lost by holding a copy — and in exchange the workspace
 * snapshot, the session writer and the file reader keep dealing in ordinary paths.
 */
class LocalLogFileExpander(
    private val cacheDirectory: File,
    private val dispatcher: CoroutineDispatcher,
) : LogFileExpander {

    override suspend fun expand(path: String): List<ExpandedLogFile> = withContext(context = dispatcher) {
        val file = File(path)
        require(value = file.isFile) { "File not found: $path" }
        when {
            !SupportedFileTypes.isArchive(path = path) -> listOf(plainFile(file = file))
            file.name.endsWith(suffix = ZIP_SUFFIX, ignoreCase = true) -> unpackZip(file = file)
            else -> listOf(unpackGzip(file = file))
        }
    }

    private fun plainFile(file: File): ExpandedLogFile = ExpandedLogFile(
        path = file.absolutePath,
        displayName = file.name,
        modifiedAt = modificationOf(millis = file.lastModified()),
    )

    /**
     * A gzip stream carries no name of its own, so the outer one is used with the suffix removed:
     * `app.log.gz` unpacks to a file still called `app.log`.
     */
    private fun unpackGzip(file: File): ExpandedLogFile {
        val name = file.name.dropLast(n = GZIP_SUFFIX.length).ifEmpty { file.name }
        val target = File(stagingFor(file = file), name)
        target.parentFile?.mkdirs()
        GZIPInputStream(file.inputStream().buffered()).use { source -> writeTo(target = target, source = source) }
        return ExpandedLogFile(
            path = target.absolutePath,
            displayName = name,
            modifiedAt = modificationOf(millis = file.lastModified()),
        )
    }

    private fun unpackZip(file: File): List<ExpandedLogFile> {
        val staging = stagingFor(file = file)
        return ZipFile(file).use { archive ->
            archive.entries().toList()
                .filter { entry -> !entry.isDirectory && isReadableLog(name = entry.name) }
                .mapNotNull { entry ->
                    val target = resolveInside(root = staging, entryName = entry.name) ?: return@mapNotNull null
                    target.parentFile?.mkdirs()
                    archive.getInputStream(entry).use { source -> writeTo(target = target, source = source) }
                    ExpandedLogFile(
                        path = target.absolutePath,
                        displayName = target.name,
                        // An entry need not record a time; the archive around it always does, and
                        // that is a far better answer for "which day is this" than none at all.
                        modifiedAt = modificationOf(millis = entry.time)
                            ?: modificationOf(millis = file.lastModified()),
                    )
                }
        }
    }

    /** A nested archive is left alone: unpacking recursively turns one chosen file into a surprise. */
    private fun isReadableLog(name: String): Boolean =
        SupportedFileTypes.accepts(kind = MediaKind.LOG, path = name) && !SupportedFileTypes.isArchive(path = name)

    /**
     * Refuses an entry whose name escapes the staging directory.
     *
     * An archive is data from elsewhere and its entry names are attacker controlled; `../../` in one
     * of them would otherwise write wherever the application can reach.
     */
    private fun resolveInside(root: File, entryName: String): File? {
        val target = File(root, entryName).canonicalFile
        val boundary = root.canonicalFile
        return target.takeIf { it.path.startsWith(prefix = boundary.path + File.separator) }
    }

    private fun writeTo(target: File, source: InputStream) {
        target.outputStream().buffered().use { sink -> source.copyTo(out = sink) }
    }

    /**
     * One directory per archive, named after it and after where it came from, so that two files of
     * the same name in different folders do not overwrite each other's entries.
     */
    private fun stagingFor(file: File): File {
        val token = file.absolutePath.hashCode().toUInt().toString(radix = HASH_RADIX)
        return File(cacheDirectory, "$UNPACKED_DIRECTORY/${file.name}-$token").also { it.mkdirs() }
    }

    /** A zip entry with no recorded time reports -1; that is an absence, not an instant in 1969. */
    private fun modificationOf(millis: Long): Instant? =
        if (millis <= 0L) null else Instant.fromEpochMilliseconds(epochMilliseconds = millis)

    private companion object {
        const val ZIP_SUFFIX = ".zip"
        const val GZIP_SUFFIX = ".gz"
        const val UNPACKED_DIRECTORY = "unpacked"
        const val HASH_RADIX = 36
    }
}
