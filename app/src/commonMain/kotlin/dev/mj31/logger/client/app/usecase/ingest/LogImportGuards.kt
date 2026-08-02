package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import kotlinx.coroutines.CancellationException

/**
 * Refuses a path whose type the workspace does not accept.
 *
 * The check lives next to the import rather than in the file dialog: a path can also arrive from the
 * command line, and some platforms ignore the filter of their native dialog.
 */
internal fun rejectionOf(path: String): LogImportResult.Failure? =
    if (SupportedFileTypes.accepts(kind = MediaKind.LOG, path = path)) {
        null
    } else {
        LogImportResult.Failure(
            path = path,
            message = SupportedFileTypes.rejectionMessage(
                kind = MediaKind.LOG,
                fileName = path.substringAfterLast(delimiter = '/').substringAfterLast(delimiter = '\\'),
            ),
        )
    }

/**
 * Keeps structured concurrency intact: [runCatching] also swallows cancellation, which would turn a
 * cancelled import into a spurious failure result.
 */
internal fun <T> Result<T>.rethrowCancellation(): Result<T> = onFailure { error ->
    if (error is CancellationException) throw error
}
