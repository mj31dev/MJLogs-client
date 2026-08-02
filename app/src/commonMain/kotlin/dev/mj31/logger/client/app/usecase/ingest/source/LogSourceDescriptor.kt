package dev.mj31.logger.client.app.usecase.ingest.source

/** Identity of a log file inside the session. */
data class LogSourceDescriptor(
    val id: String,
    val name: String,
    val path: String,
)
