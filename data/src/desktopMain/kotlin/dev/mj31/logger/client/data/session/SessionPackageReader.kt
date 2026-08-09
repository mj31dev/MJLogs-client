package dev.mj31.logger.client.data.session

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.SessionPackage
import dev.mj31.logger.client.domain.session.SessionFile
import java.io.File
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Opens session files.
 *
 * A full package is unpacked into a cache folder before anything reads it: the decoder needs a real
 * path to a real file, and nothing downstream should have to care whether the bytes came out of an
 * archive or off the file system.
 */
internal class SessionPackageReader(
    private val cacheDirectory: File,
    private val snapshotDatabase: PackageSnapshotDatabase,
) {

    suspend fun read(path: String): SessionPackage {
        val file = File(path)
        require(value = file.isFile) { "Session file not found: $path" }
        require(value = SessionFile.matches(path = path)) {
            "$path is not a session file; expected ${SessionFile.EXTENSION}"
        }
        val extracted = SessionPackageLayout.extractedDirectory(cacheDirectory = cacheDirectory, packageFile = file)
        extracted.mkdirs()
        unpack(file = file, extracted = extracted)
        val stored = snapshotDatabase.read(
            databasePath = File(extracted, SessionPackageLayout.DATABASE_ENTRY).absolutePath,
        ) ?: WorkspaceSnapshot.EMPTY
        return SessionPackage(
            path = file.absolutePath,
            name = SessionFile.nameOf(path = file.name),
            snapshot = resolve(snapshot = stored, extracted = extracted)
                .copy(packagePath = file.absolutePath),
        )
    }

    /** Drops what a previous read unpacked; the archive itself is never touched. */
    fun releaseExtracted(path: String) {
        SessionPackageLayout
            .extractedDirectory(cacheDirectory = cacheDirectory, packageFile = File(path))
            .deleteRecursively()
    }

    private fun unpack(file: File, extracted: File) {
        ZipFile(file).use { zip ->
            verify(zip = zip, path = file.path)
            val database = zip.getEntry(SessionPackageLayout.DATABASE_ENTRY)
            requireNotNull(value = database) { "${file.name} carries no workspace." }
            extract(zip = zip, entry = database, target = File(extracted, SessionPackageLayout.DATABASE_ENTRY))
            zip.entries().asSequence()
                .filter { !it.isDirectory && SessionPackageLayout.isBundledEntry(path = it.name) }
                .forEach { entry -> extract(zip = zip, entry = entry, target = File(extracted, entry.name)) }
        }
    }

    /**
     * Refuses a layout from the future rather than reading half of it.
     *
     * A newer version may have moved what this build expects to find; guessing would produce a
     * session that looks fine and is quietly missing something.
     */
    private fun verify(zip: ZipFile, path: String) {
        val entry = zip.getEntry(SessionPackageLayout.MANIFEST_ENTRY) ?: return
        val manifest = Properties()
        zip.getInputStream(entry).use { input -> manifest.load(input) }
        val version = manifest.getProperty(SessionPackageLayout.KEY_FORMAT_VERSION)?.toIntOrNull() ?: return
        require(value = version <= SessionPackageLayout.FORMAT_VERSION) {
            "$path was written in session format $version; this build understands up to " +
                "${SessionPackageLayout.FORMAT_VERSION}."
        }
    }

    private fun extract(zip: ZipFile, entry: ZipEntry, target: File) {
        // The entry names this application writes are safe by construction; the ones in a file it
        // merely opens are not, and `..` in an entry name is the oldest trick in the archive book.
        require(value = !entry.name.contains(other = "..")) { "Refusing to unpack ${entry.name}." }
        if (target.isFile && target.length() == entry.size) return
        target.parentFile?.mkdirs()
        zip.getInputStream(entry).use { input ->
            target.outputStream().buffered().use { output -> input.copyTo(out = output) }
        }
    }

    /** Points the stored workspace at the copies just unpacked. */
    private fun resolve(snapshot: WorkspaceSnapshot, extracted: File): WorkspaceSnapshot {
        return snapshot.copy(
            logSources = snapshot.logSources.map { ref ->
                if (SessionPackageLayout.isBundledEntry(path = ref.path)) {
                    ref.copy(path = File(extracted, ref.path).absolutePath)
                } else {
                    ref
                }
            },
            video = snapshot.video?.let { media ->
                if (SessionPackageLayout.isBundledEntry(path = media.path)) {
                    media.copy(path = File(extracted, media.path).absolutePath)
                } else {
                    media
                }
            },
        )
    }
}
