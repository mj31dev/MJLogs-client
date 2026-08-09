package dev.mj31.logger.client.data.session

import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.PackageWriteProgress
import dev.mj31.logger.client.domain.session.SessionFile
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Writes session files.
 *
 * Everything is built into a neighbouring `.part` file and only moved onto the target once it is
 * complete, so an interrupted save — by cancellation, by a full disk, by a crash — never leaves
 * something that looks like a session but cannot be opened.
 */
internal class SessionPackageWriter(
    private val cacheDirectory: File,
    private val snapshotDatabase: PackageSnapshotDatabase,
) {

    /** One file that goes into the archive, and the name it takes there. */
    private data class BundledFile(val source: File, val entryName: String)

    private data class Bundle(val snapshot: WorkspaceSnapshot, val files: List<BundledFile>)

    fun write(targetPath: String, snapshot: WorkspaceSnapshot): Flow<PackageWriteProgress> = flow {
        val target = File(targetPath)
        val partial = File(target.absolutePath + SessionPackageLayout.PARTIAL_SUFFIX)
        val staging = createTempStaging()
        try {
            val bundle = bundleOf(snapshot = snapshot)
            val databaseFile = stagedDatabase(staging = staging, snapshot = bundle.snapshot)
            val totalBytes = bundle.files.sumOf { it.source.length() }
            writeArchive(partial = partial, database = databaseFile, bundle = bundle, total = totalBytes)
            moveIntoPlace(partial = partial, target = target)
            emit(PackageWriteProgress(fileName = target.name, copiedBytes = totalBytes, totalBytes = totalBytes))
        } finally {
            staging.deleteRecursively()
            partial.delete()
        }
    }

    /**
     * Replaces the stored workspace of an existing package without touching what it carries.
     *
     * The archive is rebuilt because no archive format lets a single entry grow in place, but the
     * bundled media is copied over verbatim rather than re-read from the file system.
     */
    suspend fun updateSnapshot(path: String, snapshot: WorkspaceSnapshot) {
        val target = File(path)
        if (!target.isFile) return
        if (!SessionFile.matches(path = path)) return
        val partial = File(target.absolutePath + SessionPackageLayout.PARTIAL_SUFFIX)
        val staging = createTempStaging()
        try {
            val stored = toEntryPaths(snapshot = snapshot)
            val databaseFile = stagedDatabase(staging = staging, snapshot = stored)
            rebuildArchive(source = target, partial = partial, database = databaseFile)
            moveIntoPlace(partial = partial, target = target)
        } finally {
            staging.deleteRecursively()
            partial.delete()
        }
    }

    private suspend fun stagedDatabase(staging: File, snapshot: WorkspaceSnapshot): File {
        val databaseFile = File(staging, SessionPackageLayout.DATABASE_ENTRY)
        snapshotDatabase.write(databasePath = databaseFile.absolutePath, snapshot = snapshot)
        return databaseFile
    }

    private suspend fun FlowCollector<PackageWriteProgress>.writeArchive(
        partial: File,
        database: File,
        bundle: Bundle,
        total: Long,
    ) {
        partial.parentFile?.mkdirs()
        ZipOutputStream(partial.outputStream().buffered()).use { zip ->
            putManifest(zip = zip)
            putFile(zip = zip, name = SessionPackageLayout.DATABASE_ENTRY, source = database)
            var copied = 0L
            bundle.files.forEach { item ->
                copied = copyTracked(zip = zip, item = item, alreadyCopied = copied, total = total)
            }
        }
    }

    private fun rebuildArchive(source: File, partial: File, database: File) {
        ZipFile(source).use { existing ->
            ZipOutputStream(partial.outputStream().buffered()).use { zip ->
                existing.entries().asSequence()
                    .filter { it.name != SessionPackageLayout.DATABASE_ENTRY && !it.isDirectory }
                    .forEach { entry ->
                        zip.putNextEntry(ZipEntry(entry.name))
                        existing.getInputStream(entry).use { input -> input.copyTo(out = zip) }
                        zip.closeEntry()
                    }
                putFile(zip = zip, name = SessionPackageLayout.DATABASE_ENTRY, source = database)
            }
        }
    }

    /** Copies one bundled file, reporting progress often enough that cancelling feels immediate. */
    private suspend fun FlowCollector<PackageWriteProgress>.copyTracked(
        zip: ZipOutputStream,
        item: BundledFile,
        alreadyCopied: Long,
        total: Long,
    ): Long {
        emit(PackageWriteProgress(fileName = item.source.name, copiedBytes = alreadyCopied, totalBytes = total))
        zip.putNextEntry(ZipEntry(item.entryName))
        var copied = alreadyCopied
        val buffer = ByteArray(size = BUFFER_BYTES)
        item.source.inputStream().buffered().use { input ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                zip.write(buffer, 0, read)
                copied += read
                emit(PackageWriteProgress(fileName = item.source.name, copiedBytes = copied, totalBytes = total))
            }
        }
        zip.closeEntry()
        return copied
    }

    private fun putManifest(zip: ZipOutputStream) {
        val manifest = Properties()
        manifest.setProperty(SessionPackageLayout.KEY_FORMAT_VERSION, SessionPackageLayout.FORMAT_VERSION.toString())
        zip.putNextEntry(ZipEntry(SessionPackageLayout.MANIFEST_ENTRY))
        manifest.store(zip as OutputStream, "MJLogs session package")
        zip.closeEntry()
    }

    private fun putFile(zip: ZipOutputStream, name: String, source: File) {
        zip.putNextEntry(ZipEntry(name))
        source.inputStream().buffered().use { input -> input.copyTo(out = zip) }
        zip.closeEntry()
    }

    private fun moveIntoPlace(partial: File, target: File) {
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Points every reference at the entry that will carry it.
     *
     * A file that has disappeared since it was imported keeps its original path: the package then
     * degrades to a reference for that one source, which is more useful than refusing to save.
     */
    private fun bundleOf(snapshot: WorkspaceSnapshot): Bundle {
        val files = mutableListOf<BundledFile>()
        val sources = snapshot.logSources.mapIndexed { index, ref -> bundleSource(files, index, ref) }
        val video = snapshot.video?.let { media ->
            val file = File(media.path)
            if (!file.isFile) return@let media
            val entry = SessionPackageLayout.videoEntryName(fileName = media.name)
            files += BundledFile(source = file, entryName = entry)
            media.copy(path = entry)
        }
        return Bundle(snapshot = snapshot.copy(logSources = sources, video = video), files = files)
    }

    private fun bundleSource(files: MutableList<BundledFile>, index: Int, ref: LogSourceRef): LogSourceRef {
        val file = File(ref.path)
        if (!file.isFile) return ref
        val entry = SessionPackageLayout.logEntryName(index = index, fileName = ref.name)
        files += BundledFile(source = file, entryName = entry)
        return ref.copy(path = entry)
    }

    /** Turns the extracted absolute paths of an opened package back into the names it stores. */
    private fun toEntryPaths(snapshot: WorkspaceSnapshot): WorkspaceSnapshot = snapshot.copy(
        logSources = snapshot.logSources.mapIndexed { index, ref ->
            if (SessionPackageLayout.isBundledEntry(path = ref.path)) ref
            else ref.copy(path = SessionPackageLayout.logEntryName(index = index, fileName = ref.name))
        },
        video = snapshot.video?.let { media ->
            if (SessionPackageLayout.isBundledEntry(path = media.path)) media
            else media.copy(path = SessionPackageLayout.videoEntryName(fileName = media.name))
        },
    )

    private fun createTempStaging(): File =
        File(cacheDirectory, "staging-${System.nanoTime()}").apply { mkdirs() }

    private companion object {
        const val BUFFER_BYTES = 1 shl 16
    }
}
