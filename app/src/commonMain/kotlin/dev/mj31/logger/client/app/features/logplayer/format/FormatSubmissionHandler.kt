package dev.mj31.logger.client.app.features.logplayer.format

import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.features.logplayer.duplicate.DuplicateHandler
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatError
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.LogImportResult
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The two ways the format dialog is answered: keeping what detection found, or a description the
 * user wrote. Either way the file then goes through the same gate every other file does.
 */
internal class FormatSubmissionHandler(
    private val formatRequests: FormatRequestHandler,
    private val formatTools: LogPlayerFormatTools,
    private val importLogFileWithFormat: ImportLogFileWithFormatUseCase,
    private val duplicates: DuplicateHandler,
    private val scope: CoroutineScope,
) {

    fun acceptDetected() {
        val source = formatRequests.head?.detectedSource ?: return
        scope.launch {
            formatRequests.dropHead()
            duplicates.admit(source = source)
        }
    }

    fun submit() {
        val request = formatRequests.head ?: return
        when (val compiled = formatTools.compiler.compile(input = request.draft)) {
            is FormatCompilationResult.Failure -> formatRequests.showError(
                error = FormatError(message = compiled.message, field = compiled.field),
            )

            is FormatCompilationResult.Success -> scope.launch {
                val result = importLogFileWithFormat(
                    path = request.path,
                    spec = compiled.spec,
                    // Getting this far means the file was already let through once, either because
                    // its name was recognized or because the user insisted on it.
                    acceptUnsupported = true,
                )
                when (result) {
                    is LogImportResult.Success -> {
                        formatRequests.dropHead()
                        duplicates.admit(source = result.source)
                    }

                    // A format the user wrote themselves needs no confirmation of what it leaves out.
                    is LogImportResult.NeedsConfirmation -> {
                        formatRequests.dropHead()
                        duplicates.admit(source = result.source)
                    }

                    // Neither input is syntactically wrong: the format simply does not fit the file.
                    is LogImportResult.Failure -> formatRequests.showError(
                        error = FormatError(message = result.message, field = FormatErrorField.NONE),
                    )

                    is LogImportResult.FormatRequired -> formatRequests.showError(
                        error = FormatError(message = result.reason, field = FormatErrorField.NONE),
                    )

                    // Unreachable: the check above is waived. Reported rather than ignored, because
                    // a silent branch here would hide a change to the guard.
                    is LogImportResult.UnsupportedType -> formatRequests.showError(
                        error = FormatError(message = result.message, field = FormatErrorField.NONE),
                    )

                    // A format the user just wrote is applied to the day already settled for the file.
                    is LogImportResult.StartDayRequired -> formatRequests.showError(
                        error = FormatError(message = START_DAY_UNEXPECTED, field = FormatErrorField.NONE),
                    )
                }
            }
        }
    }

    private companion object {
        const val START_DAY_UNEXPECTED = "This file spans two days and needs one to be chosen first."
    }
}
