package dev.mj31.logger.client.domain.model.log

import dev.mj31.logger.client.domain.model.time.TimeRange
/**
 * Declarative description of the currently visible slice of a [LogSession].
 *
 * Every criterion is optional and combined with a logical AND. Empty collections mean "no
 * restriction", which keeps the default filter allocation cheap and trivially serializable later.
 */
data class LogFilter(
    val query: String = "",
    val levels: Set<LogLevel> = emptySet(),
    val sourceIds: Set<String> = emptySet(),
    val timeRange: TimeRange? = null,
) {

    /** Normalized once per filter instance: [matches] is called for every entry of the session. */
    private val normalizedQuery: String = query.trim()

    val isActive: Boolean
        get() = normalizedQuery.isNotEmpty() || levels.isNotEmpty() || sourceIds.isNotEmpty() || timeRange != null

    fun matches(entry: LogEntry): Boolean {
        if (levels.isNotEmpty() && entry.level !in levels) return false
        if (sourceIds.isNotEmpty() && entry.sourceId !in sourceIds) return false
        if (timeRange != null && entry.timestamp !in timeRange) return false
        if (normalizedQuery.isNotEmpty() && !entry.matchesText(query = normalizedQuery)) return false
        return true
    }

    companion object {
        val NONE: LogFilter = LogFilter()
    }
}
