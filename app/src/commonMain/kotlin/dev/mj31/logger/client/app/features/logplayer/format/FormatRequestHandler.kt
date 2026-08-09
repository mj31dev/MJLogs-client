package dev.mj31.logger.client.app.features.logplayer.format

import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatDefaults
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatError
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.usecase.ingest.LogImportResult
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The queue of files waiting for the user to say how they should be read.
 *
 * Files are asked about one at a time and in order: a dialog per unrecognized file, all opened at
 * once, would be a stack of windows about a decision that has to be made one file at a time anyway.
 *
 * It owns only the dialog state. Importing a file is a decision about the session, so it stays with
 * the store and its use cases.
 */
internal class FormatRequestHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val formatTools: LogPlayerFormatTools,
) {

    val head: FormatRequestUiState?
        get() = local.value.formatRequests.firstOrNull()

    /** The preview follows every keystroke, so it has to tolerate a half-written pattern. */
    fun updateDraft(draft: ManualFormatInput) {
        val request = head ?: return
        replaceHead(
            request = request.copy(
                timestampPattern = draft.timestampPattern,
                structureTemplate = draft.structureTemplate,
                preview = preview(draft = draft, sampleLines = request.sampleLines),
                error = null,
            ),
        )
    }

    fun dropHead() {
        local.update { it.copy(formatRequests = it.formatRequests.drop(n = 1)) }
    }

    fun showError(error: FormatError) {
        head?.let { request -> replaceHead(request = request.copy(error = error)) }
    }

    /** Opens on the inferred layout when there is one, on the neutral default otherwise. */
    fun enqueue(result: LogImportResult.FormatRequired) {
        val draft = result.suggestion ?: defaultDraft()
        enqueue(
            request = FormatRequestUiState(
                path = result.path,
                fileName = result.fileName,
                sampleLines = result.sampleLines,
                reason = result.reason,
                timestampPattern = draft.timestampPattern,
                structureTemplate = draft.structureTemplate,
                preview = preview(draft = draft, sampleLines = result.sampleLines),
                suggestion = result.suggestion,
            ),
        )
    }

    /** Same dialog as an unrecognized file, but with the parsed source ready behind the accept button. */
    fun enqueue(result: LogImportResult.NeedsConfirmation) {
        val draft = result.suggestion ?: ManualFormatInput(
            timestampPattern = result.source.format.timestampPattern,
            structureTemplate = FormatDefaults.STRUCTURE_TEMPLATE,
        )
        enqueue(
            request = FormatRequestUiState(
                path = result.source.path,
                fileName = result.source.name,
                sampleLines = result.sampleLines,
                reason = result.reason,
                timestampPattern = draft.timestampPattern,
                structureTemplate = draft.structureTemplate,
                preview = preview(draft = draft, sampleLines = result.sampleLines),
                suggestion = result.suggestion,
                detectedSource = result.source,
            ),
        )
    }

    private fun enqueue(request: FormatRequestUiState) {
        local.update { it.copy(formatRequests = it.formatRequests + request) }
    }

    private fun replaceHead(request: FormatRequestUiState) {
        local.update { current ->
            current.copy(formatRequests = listOf(request) + current.formatRequests.drop(n = 1))
        }
    }

    private fun preview(draft: ManualFormatInput, sampleLines: List<String>): FormatPreview =
        formatTools.previewer.preview(input = draft, sampleLines = sampleLines)

    private fun defaultDraft(): ManualFormatInput = ManualFormatInput(
        timestampPattern = FormatDefaults.TIMESTAMP_PATTERN,
        structureTemplate = FormatDefaults.STRUCTURE_TEMPLATE,
    )
}
