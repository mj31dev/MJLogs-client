package dev.mj31.logger.client.app.features.logplayer.state.format

import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * Neutral starting points used when no layout could be inferred from the sample.
 *
 * The names are the conventional ones rather than blanks: a user who opens the JSON inputs and finds
 * `timestamp`, `level`, `message` already there usually only has to correct one of them.
 */
object FormatDefaults {
    const val TIMESTAMP_PATTERN: String = "yyyy-MM-dd HH:mm:ss.SSS"
    const val STRUCTURE_TEMPLATE: String = "{timestamp} {level} {tag}: {message}"

    const val TIMESTAMP_KEY: String = "timestamp"
    const val LEVEL_KEY: String = "level"
    const val MESSAGE_KEY: String = "message"

    const val DELIMITER: String = ","

    /** A column written by position, which is the only form a file without a header accepts. */
    const val FIRST_COLUMN: String = "#0"

    val template: ManualFormatInput.Template = ManualFormatInput.Template(
        timestampPattern = TIMESTAMP_PATTERN,
        structureTemplate = STRUCTURE_TEMPLATE,
    )
}
