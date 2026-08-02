package dev.mj31.logger.client.domain.source

/** Raw content of a plain text log file. Records are always separated by a line break. */
data class TextFileContent(
    val path: String,
    val name: String,
    val lines: List<String>,
)
