package dev.mj31.logger.client.domain.source.archive

/**
 * Turns one chosen path into the log files it actually denotes.
 *
 * A plain file denotes itself. A compressed file denotes its content. An archive denotes every entry
 * inside it that looks like a log, because an archive of logs is the same thing as several log files
 * and this application already merges those into one chronological session.
 */
interface LogFileExpander {

    /** Returns the readable files, or an empty list when a container holds nothing readable. */
    suspend fun expand(path: String): List<ExpandedLogFile>
}
