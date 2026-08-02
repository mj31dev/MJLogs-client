package dev.mj31.logger.client.app.features.logplayer.state.format

/** Neutral starting point used when no layout could be inferred from the sample. */
object FormatDefaults {
    const val TIMESTAMP_PATTERN: String = "yyyy-MM-dd HH:mm:ss.SSS"
    const val STRUCTURE_TEMPLATE: String = "{timestamp} {level} {tag}: {message}"
}
