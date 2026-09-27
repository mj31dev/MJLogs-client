package dev.mj31.logger.client.app.features.logplayer.state.ingest

/**
 * A file the workspace does not recognize by name, waiting for the user to insist or to let it go.
 *
 * A log has no reserved extension, so the list of accepted ones is a filter and not a verdict. It
 * still earns its place: it keeps a screencast or a photo from being read as text by accident. This
 * turns the refusal into a question rather than a dead end — the one person who knows what the file
 * is gets to say so.
 */
data class UnsupportedImportUiState(
    val path: String,
    val fileName: String,
    val reason: String,
)
