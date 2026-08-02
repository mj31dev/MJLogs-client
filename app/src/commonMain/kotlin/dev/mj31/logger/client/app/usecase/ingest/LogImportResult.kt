package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.model.log.LogSource

/** Outcome of importing one log file. */
sealed interface LogImportResult {

    data class Success(val source: LogSource, val confidence: Float) : LogImportResult

    /**
     * Detection failed: the UI has to ask the user for the timestamp pattern and the line structure.
     *
     * [suggestion] carries the best inferred description of these lines, when one could be produced.
     */
    data class FormatRequired(
        val path: String,
        val fileName: String,
        val sampleLines: List<String>,
        val reason: String,
        val suggestion: ManualFormatInput? = null,
    ) : LogImportResult

    /**
     * The file was parsed, but the recognized format leaves some components out — a log with no
     * level column looks exactly like one whose level the app failed to locate, and only the user
     * can tell the two apart. [source] is ready to be added as it is, should they confirm.
     */
    data class NeedsConfirmation(
        val source: LogSource,
        val sampleLines: List<String>,
        val missing: Set<LogComponent>,
        val reason: String,
        val suggestion: ManualFormatInput? = null,
    ) : LogImportResult

    data class Failure(val path: String, val message: String) : LogImportResult
}
