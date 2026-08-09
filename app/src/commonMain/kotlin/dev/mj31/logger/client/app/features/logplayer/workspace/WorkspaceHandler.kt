package dev.mj31.logger.client.app.features.logplayer.workspace

import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerWorkspace
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.ui.PackageSaveUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.WorkspaceUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_session_missing_files
import dev.mj31.logger.client.app.resources.message_session_opened
import dev.mj31.logger.client.app.resources.message_session_save_cancelled
import dev.mj31.logger.client.app.resources.message_session_save_failed
import dev.mj31.logger.client.app.resources.message_session_saved
import dev.mj31.logger.client.app.usecase.workspace.session.OpenSessionResult
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.SessionFile
import dev.mj31.logger.client.domain.sync.SyncState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * Everything about remembering the workspace, kept out of the store the way the automatic
 * synchronization is.
 *
 * The application store is written eagerly and is the only thing standing between a crash and a lost
 * afternoon, so a change reaches it as soon as it happens. The playhead is the one exception: it
 * moves many times a second, and a store written that often would be a store written for nothing.
 */
internal class WorkspaceHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val workspace: LogPlayerWorkspace,
    private val scope: CoroutineScope,
    private val emit: (LogPlayerEffect) -> Unit,
    private val playbackPosition: () -> Long,
    /**
     * Called once a workspace has been put back in place.
     *
     * The screencast is not a repository value the way the logs are — it has to be handed to a
     * decoder and seeked — so the one part only the store can do is delegated back to it, and every
     * path that restores a workspace goes through the same call.
     */
    private val onApplied: (WorkspaceSnapshot) -> Unit,
) {

    private var saveJob: Job? = null

    /** Reopens what was on screen when the application was last closed. */
    fun continueLast() {
        scope.launch {
            val snapshot = runCatching { workspace.repository.loadLastWorkspace() }.getOrNull() ?: return@launch
            if (snapshot.isEmpty) return@launch
            apply(snapshot = snapshot)
        }
    }

    /** Empties everything so the next import arrives into a clean workspace. */
    fun startNew() {
        scope.launch {
            runCatching { workspace.clear(current = capture()) }
            local.update {
                it.copy(
                    filter = LogFilter(),
                    timeWindowMillis = null,
                    selectedEntryId = null,
                    workspace = WorkspaceUiState(),
                )
            }
            onApplied(WorkspaceSnapshot.EMPTY)
        }
    }

    /**
     * Stores anything worth reopening as soon as it changes.
     *
     * The first combined value is the state that has just been restored, and writing it back would
     * be a write that changes nothing.
     */
    suspend fun observeChanges(
        sources: Flow<List<LogSource>>,
        media: Flow<VideoMedia?>,
        syncState: Flow<SyncState>,
    ) {
        combine(
            sources,
            media,
            syncState,
            local.map { StoredInputs(it.filter, it.timeWindowMillis, it.followVideo) }.distinctUntilChanged(),
        ) { _, _, _, _ -> Unit }
            .drop(count = 1)
            .collect { markChanged() }
    }

    /** The parts of the local state that belong in a stored workspace, compared as one value. */
    private data class StoredInputs(
        val filter: LogFilter,
        val timeWindowMillis: Long?,
        val followVideo: Boolean,
    )

    /** Writes the playhead on a timer rather than on every tick it produces. */
    fun trackPlayback(positions: Flow<Long>) {
        scope.launch {
            positions.distinctUntilChanged().debounce(timeout = POSITION_WRITE_DELAY).collect { millis ->
                runCatching { workspace.persist.updatePlaybackPosition(positionMillis = millis) }
            }
        }
    }

    /** Something worth remembering changed; the file it belongs to now trails the screen. */
    fun markChanged() {
        val snapshot = capture()
        if (local.value.workspace.isBoundToPackage) {
            local.update { it.copy(workspace = it.workspace.copy(hasUnsavedChanges = true)) }
        }
        scope.launch { runCatching { workspace.persist(snapshot = snapshot) } }
    }

    fun save(targetPath: String) {
        saveJob?.cancel()
        val path = workspace.savePackage.targetPathFor(path = targetPath)
        saveJob = scope.launch {
            try {
                collectSave(path = path)
                bind(path = path)
                emit(LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = Res.string.message_session_saved)))
            } catch (cancellation: CancellationException) {
                announce(resource = Res.string.message_session_save_cancelled, isError = false)
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
                // A full disk, a read-only folder, a file pulled away mid-write: none of them is
                // worth taking the window down for, and all of them mean the same thing to the user.
                emit(
                    LogPlayerEffect.ShowMessage(
                        text = UiText.Raw(value = failure.message.orEmpty()),
                        isError = true,
                    ),
                )
                announce(resource = Res.string.message_session_save_failed, isError = true)
            } finally {
                local.update { it.copy(workspace = it.workspace.copy(save = null)) }
            }
        }
    }

    fun cancelSave() {
        saveJob?.cancel()
        saveJob = null
    }

    /** Brings the file up to date with the screen. */
    fun flush() {
        val snapshot = capture()
        if (snapshot.packagePath == null) return
        scope.launch {
            if (workspace.persist.flushToPackage(snapshot = snapshot)) {
                local.update { it.copy(workspace = it.workspace.copy(hasUnsavedChanges = false)) }
                emit(LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = Res.string.message_session_saved)))
            } else {
                announce(resource = Res.string.message_session_save_failed, isError = true)
            }
        }
    }

    fun open(path: String) {
        scope.launch {
            closeCurrent()
            when (val result = workspace.openPackage(path = path)) {
                is OpenSessionResult.Failed -> emit(
                    LogPlayerEffect.ShowMessage(text = UiText.Raw(value = result.message), isError = true),
                )

                is OpenSessionResult.Opened -> {
                    bind(path = result.session.path, name = result.session.name)
                    onApplied(result.session.snapshot)
                    emit(
                        LogPlayerEffect.ShowMessage(
                            text = UiText.Resource(
                                resource = Res.string.message_session_opened,
                                arguments = listOf(result.session.name),
                            ),
                        ),
                    )
                    reportMissing(names = result.restore.missingFileNames)
                }
            }
        }
    }

    /** Leaves the current package: everything pending written, everything unpacked removed. */
    suspend fun closeCurrent() {
        val snapshot = capture()
        if (snapshot.packagePath == null) return
        runCatching { workspace.closePackage(snapshot = snapshot) }
        local.update { it.copy(workspace = WorkspaceUiState()) }
    }

    private suspend fun collectSave(path: String) {
        workspace.savePackage(targetPath = path, snapshot = capture()).collect { progress ->
            local.update {
                it.copy(
                    workspace = it.workspace.copy(
                        save = PackageSaveUiState(
                            fileName = progress.fileName,
                            fraction = progress.fraction,
                            copiedBytes = progress.copiedBytes,
                            totalBytes = progress.totalBytes,
                        ),
                    ),
                )
            }
        }
    }

    private suspend fun apply(snapshot: WorkspaceSnapshot) {
        val restored = workspace.restore(snapshot = snapshot)
        local.update { current ->
            current.copy(
                filter = snapshot.filter,
                timeWindowMillis = snapshot.timeWindowMillis,
                followVideo = snapshot.followVideo,
                workspace = uiStateOf(path = snapshot.packagePath),
            )
        }
        onApplied(snapshot)
        reportMissing(names = restored.missingFileNames)
    }

    private fun reportMissing(names: List<String>) {
        if (names.isEmpty()) return
        emit(
            LogPlayerEffect.ShowMessage(
                text = UiText.Resource(
                    resource = Res.string.message_session_missing_files,
                    arguments = listOf(names.joinToString(separator = ", ")),
                ),
                isError = true,
            ),
        )
    }

    private fun bind(path: String, name: String? = null) {
        local.update {
            it.copy(
                workspace = WorkspaceUiState(
                    packagePath = path,
                    packageName = name ?: SessionFile.nameOf(path = path),
                    hasUnsavedChanges = false,
                ),
            )
        }
    }

    private fun uiStateOf(path: String?): WorkspaceUiState {
        if (path == null || !SessionFile.matches(path = path)) return WorkspaceUiState()
        return WorkspaceUiState(packagePath = path, packageName = SessionFile.nameOf(path = path))
    }

    private fun capture(): WorkspaceSnapshot = workspace.capture(
        filter = local.value.filter,
        timeWindowMillis = local.value.timeWindowMillis,
        followVideo = local.value.followVideo,
        videoPositionMillis = playbackPosition(),
        packagePath = local.value.workspace.packagePath,
    )

    private fun announce(resource: StringResource, isError: Boolean) {
        emit(LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = resource), isError = isError))
    }

    private companion object {
        /** Long enough that scrubbing writes once, short enough that a crash loses nothing visible. */
        val POSITION_WRITE_DELAY = 2_000.milliseconds
    }
}
