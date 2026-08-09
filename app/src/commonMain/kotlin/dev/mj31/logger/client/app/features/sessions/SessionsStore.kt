package dev.mj31.logger.client.app.features.sessions

import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The start screen, on its own MVI contour.
 *
 * "Which session to open" and "how to play the one that is open" are different questions, and the
 * player store is large enough already: folding a second screen into it would make one class own two
 * unrelated cycles.
 *
 * [SessionsIntent.Open], [SessionsIntent.ContinueLast], [SessionsIntent.StartNew] and
 * [SessionsIntent.RequestOpenFile] are deliberately not handled here. Every one of them replaces the
 * player's whole workspace, which is the player's decision to make.
 */
class SessionsStore(
    private val repository: WorkspaceRepository,
    private val scope: CoroutineScope,
) {

    /**
     * The last workspace is read on demand rather than observed.
     *
     * It is a single row written continuously as the user works, and the only moments its
     * description matters are the ones where this screen is looked at.
     */
    private val lastSession = MutableStateFlow<LastSessionUi?>(value = null)

    val state: StateFlow<SessionsState> = combine(
        repository.recentPackages,
        lastSession,
    ) { packages, last ->
        SessionsState(lastSession = last, recent = packages)
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = SessionsState())

    init {
        refresh()
    }

    /** Re-reads what the last workspace holds; called whenever the screen comes back into view. */
    fun refresh() {
        scope.launch {
            val snapshot = runCatching { repository.loadLastWorkspace() }.getOrNull()
            lastSession.value = snapshot?.takeUnless { it.isEmpty }?.let(::describe)
        }
    }

    fun handleIntent(intent: SessionsIntent) {
        when (intent) {
            is SessionsIntent.Forget -> scope.launch { repository.forgetPackage(path = intent.path) }
            is SessionsIntent.Open,
            SessionsIntent.RequestOpenFile,
            SessionsIntent.ContinueLast,
            SessionsIntent.StartNew,
            -> Unit
        }
    }

    /**
     * Names the workspace after the file a person would recognize it by.
     *
     * The screencast first: someone who recorded a run remembers the recording. Failing that, the
     * first log, which is what a workspace assembled from log files alone is about.
     */
    private fun describe(snapshot: WorkspaceSnapshot): LastSessionUi = LastSessionUi(
        label = snapshot.video?.name
            ?: snapshot.logSources.firstOrNull()?.name.orEmpty(),
        logCount = snapshot.logSources.size,
        hasVideo = snapshot.video != null,
    )
}
