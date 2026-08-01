package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun getLogs(): List<LogEntry>
    fun observeLogs(): Flow<List<LogEntry>>
    suspend fun addLog(entry: LogEntry)
    suspend fun clearLogs()
}
