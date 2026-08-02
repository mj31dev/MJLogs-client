package dev.mj31.logger.client.app.features.logplayer.state.ui

/** Presentation model of one imported log file. */
data class LogSourceUi(
    val id: String,
    val name: String,
    val formatName: String,
    val entryCount: Int,
    val skippedLineCount: Int,
    val isSelected: Boolean,
)
