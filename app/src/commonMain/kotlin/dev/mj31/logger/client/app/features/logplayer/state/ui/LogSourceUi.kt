package dev.mj31.logger.client.app.features.logplayer.state.ui

import dev.mj31.logger.client.domain.model.time.SourceZone

/** Presentation model of one imported log file. */
data class LogSourceUi(
    val id: String,
    val name: String,
    val formatName: String,
    val entryCount: Int,
    val skippedLineCount: Int,
    val isSelected: Boolean,
    val hasPreamble: Boolean = false,
    val zone: SourceZone = SourceZone.UTC,
    /** How many files the source was read from: more than one once overlapping files were merged. */
    val fileCount: Int = 1,
)
