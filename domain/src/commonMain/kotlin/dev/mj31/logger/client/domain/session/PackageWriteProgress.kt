package dev.mj31.logger.client.domain.session

/**
 * How far writing a session file has got.
 *
 * A full package copies a screencast that routinely runs to hundreds of megabytes, so the operation
 * is long enough that the window has to stay alive and show what it is doing.
 */
data class PackageWriteProgress(
    val fileName: String,
    val copiedBytes: Long,
    val totalBytes: Long,
) {

    /** Zero when nothing is known about the total, so the bar can fall back to indeterminate. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (copiedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}
