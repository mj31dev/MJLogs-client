package dev.mj31.logger.client.app.features.logplayer.state.ingest

/**
 * The text a log file carries before its first record, open for reading.
 *
 * [lines] are verbatim: a preamble has no format of its own, so it is shown exactly as written
 * rather than interpreted.
 */
data class SourcePreambleUiState(
    val sourceId: String,
    val fileName: String,
    val lines: List<String>,
)
