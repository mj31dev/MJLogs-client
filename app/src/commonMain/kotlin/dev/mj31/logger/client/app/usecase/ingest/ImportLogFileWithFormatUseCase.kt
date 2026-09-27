package dev.mj31.logger.client.app.usecase.ingest

import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.usecase.ingest.date.buildPlaced
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.source.archive.LogFileExpander
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Imports a log file with a format supplied by the user after detection failed.
 *
 * The path handed here has already been through the expander once, so it denotes an ordinary file;
 * it is expanded again only to recover the modification time, which is what decides the day records
 * without a date of their own belong to.
 */
class ImportLogFileWithFormatUseCase(
    private val loader: LogSourceLoader,
    private val expander: LogFileExpander,
    private val resolveReferenceDate: ResolveReferenceDateUseCase,
    private val dispatcher: CoroutineDispatcher,
) {

    /**
     * @param referenceDate the day records without one belong to, once the user has settled it;
     * otherwise it is worked out from the file the way a fresh import does.
     */
    suspend operator fun invoke(
        path: String,
        spec: LogFormatSpec,
        acceptUnsupported: Boolean = false,
        referenceDate: LocalDate? = null,
    ): LogImportResult = withContext(context = dispatcher) {
            if (!acceptUnsupported) {
                rejectionOf(path = path)?.let { rejection -> return@withContext rejection }
            }

            val file = runCatching { expander.expand(path = path).firstOrNull() }
                .rethrowCancellation()
                .getOrNull()
                ?: return@withContext LogImportResult.Failure(path = path, message = "Unable to read file")

            val content = runCatching { loader.read(path = file.path) }
                .rethrowCancellation()
                .getOrElse { error ->
                    return@withContext LogImportResult.Failure(
                        path = path,
                        message = error.message ?: "Unable to read file",
                    )
                }

            val source = runCatching {
                resolveReferenceDate.buildPlaced(
                    loader = loader,
                    content = content,
                    spec = spec,
                    displayName = file.displayName,
                    modifiedAt = file.modifiedAt,
                    statedDay = referenceDate,
                )?.first
            }
                .rethrowCancellation()
                .getOrElse { error ->
                    return@withContext LogImportResult.Failure(
                        path = path,
                        message = error.message ?: "Invalid log format",
                    )
                }

            when {
                source == null -> LogImportResult.Failure(
                    path = path,
                    message = "The day ${file.displayName} was written could not be determined",
                )

                source.entries.isEmpty() -> LogImportResult.Failure(
                    path = path,
                    message = "No line matched the provided format",
                )

                else -> LogImportResult.Success(source = source, confidence = 1f)
            }
        }
}
