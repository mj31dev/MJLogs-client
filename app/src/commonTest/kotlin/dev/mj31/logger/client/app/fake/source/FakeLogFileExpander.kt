package dev.mj31.logger.client.app.fake.source

import dev.mj31.logger.client.domain.source.archive.ExpandedLogFile
import dev.mj31.logger.client.domain.source.archive.LogFileExpander
import kotlin.time.Instant

/**
 * Expands a path into itself, which is what the real one does for an ordinary file.
 *
 * [entries] lets a test stand in for an archive: a path registered there expands into the files it
 * names instead, without any of them having to exist on a disk.
 */
class FakeLogFileExpander(
    private val entries: Map<String, List<ExpandedLogFile>> = emptyMap(),
    private val modifiedAt: Instant? = DEFAULT_MODIFICATION,
) : LogFileExpander {

    override suspend fun expand(path: String): List<ExpandedLogFile> = entries[path] ?: listOf(
        ExpandedLogFile(
            path = path,
            displayName = path.substringAfterLast(delimiter = '/').substringAfterLast(delimiter = '\\'),
            modifiedAt = modifiedAt,
        ),
    )

    companion object {

        /** 15 January 2024, 10:00 UTC — the day the fixtures of this suite are written against. */
        val DEFAULT_MODIFICATION: Instant = Instant.fromEpochMilliseconds(epochMilliseconds = 1_705_312_800_000L)
    }
}
