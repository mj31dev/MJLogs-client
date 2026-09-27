package dev.mj31.logger.client.app.usecase.ingest

import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
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
        val suggestion: ManualFormatInput.Template? = null,
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
        val suggestion: ManualFormatInput.Template? = null,
    ) : LogImportResult

    /**
     * The name of the file is not one the workspace recognizes.
     *
     * Kept apart from [Failure] because it is not a failure of the file but of a guess about it: a
     * log carries no reserved extension, and the user may know better. Answering yes re-runs the
     * import with the check waived.
     */
    data class UnsupportedType(
        val path: String,
        val fileName: String,
        val message: String,
    ) : LogImportResult

    /**
     * The records carry no date, and two days fit the file equally well.
     *
     * Reading it either way would be a guess that is wrong half the time and silent about it, so the
     * one person who can settle it is asked. [candidates] holds exactly two days, and [spec] is the
     * format already worked out, so answering costs a re-read and no further questions.
     */
    data class StartDayRequired(
        val path: String,
        val fileName: String,
        val candidates: List<LocalDate>,
        val spec: LogFormatSpec,
    ) : LogImportResult

    data class Failure(val path: String, val message: String) : LogImportResult
}
