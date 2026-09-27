package dev.mj31.logger.client.app.features.logplayer.ingest

import dev.mj31.logger.client.app.features.logplayer.duplicate.DuplicateHandler
import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.features.logplayer.state.ingest.StartDayRequestUiState
import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.format.FormatRequestHandler
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.UnsupportedImportUiState
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.ingest.LogImportResult
import dev.mj31.logger.client.app.view.text.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything between a chosen path and a source in the session.
 *
 * One choice can turn into several logs — an archive holds many — and each of them can end
 * differently: read, needing a format, or refused because its name is not one the workspace knows.
 * That last case is a question rather than a dead end, since a log carries no reserved extension, so
 * the refusals queue up here and are put to the user one file at a time.
 *
 * Deciding *how* to read a file stays with the use case; this only routes what comes back.
 */
internal class LogImportHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val importLogFile: ImportLogFileUseCase,
    private val importLogFileWithFormat: ImportLogFileWithFormatUseCase,
    private val duplicates: DuplicateHandler,
    private val formatRequests: FormatRequestHandler,
    private val scope: CoroutineScope,
    private val emit: (LogPlayerEffect) -> Unit,
) {

    fun importAll(paths: List<String>) {
        if (paths.isEmpty()) return
        run(paths = paths, acceptUnsupported = false)
    }

    /**
     * Reads the file at the head of the queue, this time without the check on its name.
     *
     * Nothing was opened when it was refused, so insisting costs one read rather than a second one.
     */
    fun confirmUnsupported() {
        val pending = head ?: return
        dismissUnsupported()
        run(paths = listOf(pending.path), acceptUnsupported = true)
    }

    fun dismissUnsupported() {
        local.update { it.copy(unsupportedImports = it.unsupportedImports.drop(n = 1)) }
    }

    private fun run(paths: List<String>, acceptUnsupported: Boolean) {
        scope.launch {
            local.update { it.copy(isImporting = true) }
            paths.filterNot { path -> duplicates.isAlreadyOpen(path = path) }.forEach { path ->
                importLogFile(path = path, acceptUnsupported = acceptUnsupported)
                    .forEach { result -> handle(result = result) }
            }
            local.update { it.copy(isImporting = false) }
        }
    }

    private suspend fun handle(result: LogImportResult) {
        when (result) {
            is LogImportResult.Success -> duplicates.admit(source = result.source)

            is LogImportResult.FormatRequired -> formatRequests.enqueue(result = result)
            is LogImportResult.NeedsConfirmation -> formatRequests.enqueue(result = result)
            is LogImportResult.UnsupportedType -> enqueueUnsupported(result = result)
            is LogImportResult.StartDayRequired -> enqueueStartDay(result = result)
            is LogImportResult.Failure -> emit(
                LogPlayerEffect.ShowMessage(text = UiText.Raw(value = result.message), isError = true),
            )
        }
    }

    /**
     * Reads the file again on the day the user picked.
     *
     * The format was already worked out before the question was asked, so this re-read runs it
     * straight through rather than starting detection over.
     */
    fun chooseStartDay(day: LocalDate) {
        val pending = startDayHead ?: return
        dismissStartDay()
        scope.launch {
            local.update { it.copy(isImporting = true) }
            handle(
                result = importLogFileWithFormat(
                    path = pending.path,
                    spec = pending.format,
                    acceptUnsupported = true,
                    referenceDate = day,
                ),
            )
            local.update { it.copy(isImporting = false) }
        }
    }

    fun dismissStartDay() {
        local.update { it.copy(startDayRequests = it.startDayRequests.drop(n = 1)) }
    }

    private fun enqueueStartDay(result: LogImportResult.StartDayRequired) {
        local.update { state ->
            state.copy(
                startDayRequests = state.startDayRequests + StartDayRequestUiState(
                    path = result.path,
                    fileName = result.fileName,
                    candidates = result.candidates,
                    format = result.spec,
                ),
            )
        }
    }

    private fun enqueueUnsupported(result: LogImportResult.UnsupportedType) {
        local.update { state ->
            state.copy(
                unsupportedImports = state.unsupportedImports + UnsupportedImportUiState(
                    path = result.path,
                    fileName = result.fileName,
                    reason = result.message,
                ),
            )
        }
    }

    private val head: UnsupportedImportUiState?
        get() = local.value.unsupportedImports.firstOrNull()

    private val startDayHead: StartDayRequestUiState?
        get() = local.value.startDayRequests.firstOrNull()
}
