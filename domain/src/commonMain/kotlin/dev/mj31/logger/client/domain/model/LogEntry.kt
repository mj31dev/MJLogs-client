package dev.mj31.logger.client.domain.model

import kotlinx.datetime.Instant

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
}

data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val payloadJson: String? = null,
)
