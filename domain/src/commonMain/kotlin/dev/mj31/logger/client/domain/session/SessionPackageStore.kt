package dev.mj31.logger.client.domain.session

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import kotlinx.coroutines.flow.Flow

/** Reads and writes the portable session files the user saves and hands around. */
interface SessionPackageStore {

    /**
     * Writes [snapshot] into [targetPath], copying every log and the screencast into the archive.
     *
     * Cancelling the collection cancels the write and removes the half-written file: a truncated
     * archive sitting next to the real ones is worse than no file at all.
     */
    fun write(targetPath: String, snapshot: WorkspaceSnapshot): Flow<PackageWriteProgress>

    suspend fun read(path: String): SessionPackage

    /**
     * Replaces only the stored snapshot of an existing file, leaving the bundled media untouched.
     *
     * This is what makes writing a filter change cheap: rebuilding the archive would mean copying
     * every byte of the screencast again to change a few hundred of metadata.
     */
    suspend fun updateSnapshot(path: String, snapshot: WorkspaceSnapshot)

    /** Discards whatever a previous [read] unpacked for [path]. */
    suspend fun releaseExtracted(path: String)
}
