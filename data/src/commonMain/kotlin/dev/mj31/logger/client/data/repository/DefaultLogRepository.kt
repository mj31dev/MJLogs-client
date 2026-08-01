package dev.mj31.logger.client.data.repository

import dev.mj31.logger.client.domain.model.LogEntry
import dev.mj31.logger.client.domain.model.LogLevel
import dev.mj31.logger.client.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class DefaultLogRepository : LogRepository {

    private val logsState = MutableStateFlow<List<LogEntry>>(value = initialMockLogs())

    override suspend fun getLogs(): List<LogEntry> = logsState.value

    override fun observeLogs(): Flow<List<LogEntry>> = logsState.asStateFlow()

    override suspend fun addLog(entry: LogEntry) {
        logsState.value = logsState.value + entry
    }

    override suspend fun clearLogs() {
        logsState.value = emptyList()
    }

    private fun initialMockLogs(): List<LogEntry> {
        val now = Clock.System.now()
        return listOf(
            LogEntry(
                id = "1",
                timestamp = now,
                level = LogLevel.INFO,
                tag = "NetworkManager",
                message = "Connecting to socket host 127.0.0.1:8080...",
            ),
            LogEntry(
                id = "2",
                timestamp = now,
                level = LogLevel.DEBUG,
                tag = "AuthRepository",
                message = "Token refreshed successfully.",
            ),
            LogEntry(
                id = "3",
                timestamp = now,
                level = LogLevel.WARN,
                tag = "CacheStore",
                message = "Memory cache limit exceeded, purging 15 LRU items.",
            ),
            LogEntry(
                id = "4",
                timestamp = now,
                level = LogLevel.ERROR,
                tag = "DatabaseEngine",
                message = "Failed to acquire lock for transaction #1042.",
            ),
            LogEntry(
                id = "5",
                timestamp = now,
                level = LogLevel.FATAL,
                tag = "Application",
                message = "Unhandled Exception: OutOfMemoryError in buffer pipeline.",
            ),
        )
    }
}
