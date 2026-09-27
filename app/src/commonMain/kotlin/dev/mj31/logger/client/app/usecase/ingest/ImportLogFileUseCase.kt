package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.app.usecase.ingest.date.StartDayResolution
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveStartDayUseCase
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.usecase.ingest.date.ReferenceDateOrigin
import dev.mj31.logger.client.app.usecase.ingest.date.buildPlaced
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.format.spec.TimestampPatternTokens
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.archive.ExpandedLogFile
import dev.mj31.logger.client.domain.source.archive.LogFileExpander
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Imports what one chosen path denotes, detecting the format of each file automatically.
 *
 * The result is a list because a single choice can be several logs: an archive of rotated files is
 * the same thing as picking those files one by one, and the session merges them either way.
 */
class ImportLogFileUseCase(
    private val loader: LogSourceLoader,
    private val detector: LogFormatDetector,
    private val expander: LogFileExpander,
    private val resolveReferenceDate: ResolveReferenceDateUseCase,
    private val resolveStartDay: ResolveStartDayUseCase,
    private val dispatcher: CoroutineDispatcher,
) {

    /**
     * @param acceptUnsupported waives the check on the file name, after the user has insisted on a
     * file whose extension the workspace does not know.
     */
    suspend operator fun invoke(
        path: String,
        acceptUnsupported: Boolean = false,
    ): List<LogImportResult> = withContext(context = dispatcher) {
        if (!acceptUnsupported) {
            rejectionOf(path = path)?.let { rejection -> return@withContext listOf(rejection) }
        }

        val files = runCatching { expander.expand(path = path) }
            .rethrowCancellation()
            .getOrElse { error ->
                return@withContext listOf(failure(path = path, error = error, fallback = "Unable to read file"))
            }
        if (files.isEmpty()) {
            return@withContext listOf(
                LogImportResult.Failure(path = path, message = "No log files were found inside this archive"),
            )
        }

        files.map { file -> importOne(file = file) }
    }

    private suspend fun importOne(file: ExpandedLogFile): LogImportResult {
        val content = runCatching { loader.read(path = file.path) }
            .rethrowCancellation()
            .getOrElse { error ->
                return failure(path = file.path, error = error, fallback = "Unable to read file")
            }

        val meaningfulLines = content.lines.filter { it.isNotBlank() }
        if (meaningfulLines.isEmpty()) {
            return LogImportResult.Failure(path = file.path, message = "File contains no log lines")
        }

        return interpret(file = file, meaningfulLines = meaningfulLines, content = content)
    }

    private fun interpret(
        file: ExpandedLogFile,
        meaningfulLines: List<String>,
        content: TextFileContent,
    ): LogImportResult {
        val detection = detector.detect(sampleLines = meaningfulLines.take(n = SAMPLE_SIZE))
        if (detection is FormatDetectionResult.Undetermined) {
            return LogImportResult.FormatRequired(
                path = file.path,
                fileName = file.displayName,
                sampleLines = detection.sampleLines,
                reason = detection.reason,
                suggestion = detection.suggestion,
            )
        }

        val detected = detection as FormatDetectionResult.Detected
        if (TimestampPatternTokens.isHourAmbiguous(pattern = detected.spec.timestampPattern)) {
            // A twelve hour reading with nothing to tell morning from afternoon denotes two moments
            // twelve hours apart. That is an incomplete description of the format rather than an
            // unanswerable question, so it goes where incomplete descriptions already go.
            return LogImportResult.FormatRequired(
                path = file.path,
                fileName = file.displayName,
                sampleLines = meaningfulLines.take(n = PREVIEW_SIZE),
                reason = TWELVE_HOUR_REASON,
            )
        }

        val source = when (val placement = place(content = content, spec = detected.spec, file = file)) {
            Placement.Undated -> return LogImportResult.Failure(
                path = file.path,
                message = "The day ${file.displayName} was written could not be determined",
            )

            is Placement.NeedsDay -> return LogImportResult.StartDayRequired(
                path = file.path,
                fileName = file.displayName,
                candidates = placement.candidates,
                spec = detected.spec,
            )

            is Placement.Built -> placement.source
        }

        return when {
            source.entries.isEmpty() -> LogImportResult.FormatRequired(
                path = file.path,
                fileName = file.displayName,
                sampleLines = meaningfulLines.take(n = PREVIEW_SIZE),
                reason = "Detected format produced no records",
            )

            detected.missingComponents.isNotEmpty() -> LogImportResult.NeedsConfirmation(
                source = source,
                sampleLines = meaningfulLines.take(n = PREVIEW_SIZE),
                missing = detected.missingComponents,
                reason = confirmationReason(detection = detected),
                suggestion = detected.suggestion,
            )

            else -> LogImportResult.Success(source = source, confidence = detected.confidence)
        }
    }

    /** Either the source, the two days between which only the user can choose, or no day at all. */
    private sealed interface Placement {
        data class Built(val source: LogSource) : Placement
        data class NeedsDay(val candidates: List<LocalDate>) : Placement
        data object Undated : Placement
    }

    /**
     * Builds the source and checks the day it was placed on, rebuilding it once when a different day
     * is the only one that fits.
     *
     * A day the file states in its own preamble is not checked: it dates the first record directly,
     * whereas the question being settled is how far a guess from the name or the last write is off.
     */
    private fun place(
        content: TextFileContent,
        spec: LogFormatSpec,
        file: ExpandedLogFile,
    ): Placement {
        val (assumed, origin) = resolveReferenceDate.buildPlaced(
            loader = loader,
            content = content,
            spec = spec,
            displayName = file.displayName,
            modifiedAt = file.modifiedAt,
        ) ?: return Placement.Undated
        if (origin == ReferenceDateOrigin.HEADER) return Placement.Built(source = assumed)
        val assumedStart = assumed.referenceDate
        val resolution = resolveStartDay(
            source = assumed,
            assumedStart = assumedStart,
            modifiedAt = file.modifiedAt,
        )
        return when (resolution) {
            is StartDayResolution.Ambiguous -> Placement.NeedsDay(candidates = resolution.candidates)
            is StartDayResolution.Settled -> Placement.Built(
                source = if (resolution.day == assumedStart) {
                    assumed
                } else {
                    loader.buildSource(
                        content = content,
                        spec = spec,
                        referenceDate = resolution.day,
                        sourceId = assumed.id,
                        displayName = file.displayName,
                        zone = assumed.zone,
                    )
                },
            )
        }
    }

    private fun failure(path: String, error: Throwable, fallback: String): LogImportResult.Failure =
        LogImportResult.Failure(path = path, message = error.message ?: fallback)

    private fun confirmationReason(detection: FormatDetectionResult.Detected): String {
        val missing = detection.missingComponents.joinToString(separator = " and ") { it.name.lowercase() }
        return "Recognized as \"${detection.spec.name}\", but no $missing could be located in these lines. " +
            "Confirm that the file really has none, or describe the layout yourself."
    }

    private companion object {
        const val TWELVE_HOUR_REASON = "The times in this file are on a twelve hour clock with no AM or " +
            "PM marker, so each one could mean either of two moments. Add the marker to the pattern, " +
            "or describe the times as a twenty-four hour clock."
        const val SAMPLE_SIZE = 200
        const val PREVIEW_SIZE = 8
    }
}
