package dev.mj31.logger.client.app.features.logplayer.state.format

import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * The shape a log is being described as, and therefore which inputs the dialog shows.
 *
 * Detection guesses this, and guesses wrong exactly when the user opens the dialog — which is why
 * the choice belongs inside it. Switching replaces the draft rather than editing it: a structure
 * template and a set of JSON keys have nothing to carry over from one another.
 */
enum class FormatKind {
    TEMPLATE,
    JSON,
    DELIMITED,
    ;

    /** A draft of this shape, opened on values that already parse most logs of that shape. */
    fun emptyDraft(timestampPattern: String): ManualFormatInput = when (this) {
        TEMPLATE -> ManualFormatInput.Template(
            timestampPattern = timestampPattern,
            structureTemplate = FormatDefaults.STRUCTURE_TEMPLATE,
        )

        JSON -> ManualFormatInput.Json(
            timestampPattern = timestampPattern,
            timestampKey = FormatDefaults.TIMESTAMP_KEY,
            levelKey = FormatDefaults.LEVEL_KEY,
            messageKey = FormatDefaults.MESSAGE_KEY,
        )

        DELIMITED -> ManualFormatInput.Delimited(
            timestampPattern = timestampPattern,
            delimiter = FormatDefaults.DELIMITER,
            hasHeader = false,
            timestampField = FormatDefaults.FIRST_COLUMN,
        )
    }

    companion object {

        fun of(input: ManualFormatInput): FormatKind = when (input) {
            is ManualFormatInput.Template -> TEMPLATE
            is ManualFormatInput.Json -> JSON
            is ManualFormatInput.Delimited -> DELIMITED
        }
    }
}
