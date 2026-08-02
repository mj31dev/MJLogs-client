package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader

/** Imports a log file, detecting its format automatically. */
class ImportLogFileUseCase(
    private val loader: LogSourceLoader,
    private val detector: LogFormatDetector,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(path: String): LogImportResult = withContext(context = dispatcher) {
        rejectionOf(path = path)?.let { rejection -> return@withContext rejection }
        val content = runCatching { loader.read(path = path) }
            .rethrowCancellation()
            .getOrElse { error ->
                return@withContext LogImportResult.Failure(
                    path = path,
                    message = error.message ?: "Unable to read file",
                )
            }

        val meaningfulLines = content.lines.filter { it.isNotBlank() }
        if (meaningfulLines.isEmpty()) {
            return@withContext LogImportResult.Failure(path = path, message = "File contains no log lines")
        }

        when (val detection = detector.detect(sampleLines = meaningfulLines.take(n = SAMPLE_SIZE))) {
            is FormatDetectionResult.Detected -> {
                val source = loader.buildSource(content = content, spec = detection.spec)
                when {
                    source.entries.isEmpty() -> LogImportResult.FormatRequired(
                        path = path,
                        fileName = content.name,
                        sampleLines = meaningfulLines.take(n = PREVIEW_SIZE),
                        reason = "Detected format produced no records",
                    )

                    detection.missingComponents.isNotEmpty() -> LogImportResult.NeedsConfirmation(
                        source = source,
                        sampleLines = meaningfulLines.take(n = PREVIEW_SIZE),
                        missing = detection.missingComponents,
                        reason = confirmationReason(detection = detection),
                        suggestion = detection.suggestion,
                    )

                    else -> LogImportResult.Success(source = source, confidence = detection.confidence)
                }
            }

            is FormatDetectionResult.Undetermined -> LogImportResult.FormatRequired(
                path = path,
                fileName = content.name,
                sampleLines = detection.sampleLines,
                reason = detection.reason,
                suggestion = detection.suggestion,
            )
        }
    }

    private fun confirmationReason(detection: FormatDetectionResult.Detected): String {
        val missing = detection.missingComponents.joinToString(separator = " and ") { it.name.lowercase() }
        return "Recognized as \"${detection.spec.name}\", but no $missing could be located in these lines. " +
            "Confirm that the file really has none, or describe the layout yourself."
    }

    private companion object {
        const val SAMPLE_SIZE = 200
        const val PREVIEW_SIZE = 8
    }
}
