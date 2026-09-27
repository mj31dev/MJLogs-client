package dev.mj31.logger.client.app.features.logplayer.duplicate

import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateChoice
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateKind
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_already_open
import dev.mj31.logger.client.app.resources.message_import_success
import dev.mj31.logger.client.app.resources.message_merged
import dev.mj31.logger.client.app.usecase.ingest.duplicate.DetectDuplicateUseCase
import dev.mj31.logger.client.app.usecase.ingest.duplicate.DuplicateVerdict
import dev.mj31.logger.client.app.usecase.ingest.duplicate.MergeSourcePartsUseCase
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The last gate between a file that was read and the session.
 *
 * Every way a file arrives — detected, described by hand, placed on a chosen day — ends here, so a
 * file already open is never opened twice by one route while another checks. The same path is
 * refused outright; a copy or an overlap is a question, queued like the other import questions and
 * put one file at a time.
 *
 * Deciding what counts as a duplicate, and how two files merge, stays with the use cases; this only
 * routes the verdict.
 */
internal class DuplicateHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val sessionRepository: LogSessionRepository,
    private val detectDuplicate: DetectDuplicateUseCase,
    private val mergeParts: MergeSourcePartsUseCase,
    private val scope: CoroutineScope,
    private val emit: (LogPlayerEffect) -> Unit,
) {

    /**
     * True, with the user told so, when [path] is already open.
     *
     * Asked before a file is read, so that a file already on screen does not cost a read, and does
     * not raise a format question about a log the user is already looking at.
     */
    fun isAlreadyOpen(path: String): Boolean {
        val open = detectDuplicate(path = path, sources = sessionRepository.sources.value) ?: return false
        emit(alreadyOpen(name = open.name))
        return true
    }

    suspend fun admit(source: LogSource) {
        when (val verdict = detectDuplicate(candidate = source, sources = sessionRepository.sources.value)) {
            DuplicateVerdict.Distinct -> add(source = source)
            is DuplicateVerdict.SamePath -> emit(alreadyOpen(name = source.name))
            is DuplicateVerdict.SameContent -> enqueue(
                kind = DuplicateKind.COPY,
                candidate = source,
                existing = verdict.existing,
                shared = source.entryCount,
            )
            is DuplicateVerdict.Overlap -> enqueue(
                kind = DuplicateKind.OVERLAP,
                candidate = source,
                existing = verdict.existing,
                shared = verdict.sharedRecordCount,
            )
        }
    }

    fun resolve(choice: DuplicateChoice) {
        val request = local.value.duplicateRequests.firstOrNull() ?: return
        local.update { it.copy(duplicateRequests = it.duplicateRequests.drop(n = 1)) }
        scope.launch {
            when (choice) {
                DuplicateChoice.SKIP -> Unit
                DuplicateChoice.ADD_SEPARATELY -> add(source = request.candidate)
                DuplicateChoice.MERGE -> merge(request = request)
            }
        }
    }

    /** A file closed while the question was open leaves nothing to merge into, so it simply opens. */
    private suspend fun merge(request: DuplicateRequestUiState) {
        val existing = sessionRepository.sources.value.firstOrNull { it.id == request.existingSourceId }
            ?: return add(source = request.candidate)
        val merged = mergeParts(base = existing, addition = request.candidate)
        sessionRepository.replaceSource(source = merged)
        emit(
            LogPlayerEffect.ShowMessage(
                text = UiText.Resource(
                    resource = Res.string.message_merged,
                    arguments = listOf(request.candidate.name, existing.name, merged.entryCount - existing.entryCount),
                ),
            ),
        )
    }

    private suspend fun add(source: LogSource) {
        sessionRepository.addSource(source = source)
        emit(
            LogPlayerEffect.ShowMessage(
                text = UiText.Resource(
                    resource = Res.string.message_import_success,
                    arguments = listOf(source.name, source.entryCount, source.format.name),
                ),
            ),
        )
    }

    private fun enqueue(kind: DuplicateKind, candidate: LogSource, existing: LogSource, shared: Int) {
        local.update { state ->
            state.copy(
                duplicateRequests = state.duplicateRequests + DuplicateRequestUiState(
                    kind = kind,
                    candidate = candidate,
                    existingSourceId = existing.id,
                    existingName = existing.name,
                    existingPath = existing.path,
                    sharedRecordCount = shared,
                ),
            )
        }
    }

    private fun alreadyOpen(name: String): LogPlayerEffect = LogPlayerEffect.ShowMessage(
        text = UiText.Resource(resource = Res.string.message_already_open, arguments = listOf(name)),
        isError = true,
    )
}
