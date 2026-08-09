package dev.mj31.logger.client.app.usecase.workspace

import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository

/**
 * Rebuilds a workspace from its description.
 *
 * Each file is read again under the format it was stored with, so no detection runs and no dialog
 * opens: the question of how to read this file was answered when it was first imported, and asking
 * it again on every launch would be asking the user to confirm their own past decisions.
 *
 * Identifiers are reused rather than regenerated, which is what keeps a stored filter by source
 * pointing at the same files it was written against.
 */
class RestoreWorkspaceUseCase(
    private val loader: LogSourceLoader,
    private val sessionRepository: LogSessionRepository,
    private val videoRepository: VideoRepository,
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(snapshot: WorkspaceSnapshot): WorkspaceRestoreResult {
        sessionRepository.clear()
        val restored = mutableListOf<LogSource>()
        val missing = mutableListOf<String>()
        snapshot.logSources.forEach { ref ->
            val source = read(ref = ref)
            if (source == null) missing += ref.name else restored += source
        }
        restored.forEach { source -> sessionRepository.addSource(source = source) }
        videoRepository.setMedia(media = snapshot.video)
        // A restored anchor is used as it stands. Finding it again costs an optical scan of the
        // screencast, and the result would be the same one already stored.
        snapshot.anchor?.let { anchor -> syncRepository.setAnchor(anchor = anchor) } ?: syncRepository.clearAnchor()
        return WorkspaceRestoreResult(restoredSourceCount = restored.size, missingFileNames = missing)
    }

    /** A file that no longer reads is not an error of the workspace; the rest of it still opens. */
    private suspend fun read(ref: LogSourceRef): LogSource? = runCatching {
        loader.buildSource(
            content = loader.read(path = ref.path),
            spec = ref.format,
            sourceId = ref.id,
        )
    }.getOrNull()
}
