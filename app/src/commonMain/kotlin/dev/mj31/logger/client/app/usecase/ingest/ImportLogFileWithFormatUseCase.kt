package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader

/** Imports a log file with a format supplied by the user after detection failed. */
class ImportLogFileWithFormatUseCase(
    private val loader: LogSourceLoader,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(path: String, spec: LogFormatSpec): LogImportResult =
        withContext(context = dispatcher) {
            rejectionOf(path = path)?.let { rejection -> return@withContext rejection }
            val content = runCatching { loader.read(path = path) }
                .rethrowCancellation()
                .getOrElse { error ->
                    return@withContext LogImportResult.Failure(
                        path = path,
                        message = error.message ?: "Unable to read file",
                    )
                }

            val source = runCatching { loader.buildSource(content = content, spec = spec) }
                .rethrowCancellation()
                .getOrElse { error ->
                    return@withContext LogImportResult.Failure(
                        path = path,
                        message = error.message ?: "Invalid log format",
                    )
                }

            if (source.entries.isEmpty()) {
                LogImportResult.Failure(path = path, message = "No line matched the provided format")
            } else {
                LogImportResult.Success(source = source, confidence = 1f)
            }
        }
}
