package dev.mj31.logger.client.domain.format.compile

/**
 * Format description entered by the user when detection fails.
 *
 * One variant per shape a log can have, mirroring the specification it compiles into. Everything
 * here is raw text as typed, not a validated structure: the dialog has to be able to hold a
 * half-written pattern between two keystrokes, and a rejection has to be able to point at the input
 * that caused it.
 *
 * A blank field means the component is absent, which is an ordinary thing for a log to be missing —
 * only the timestamp is required.
 */
sealed interface ManualFormatInput {

    /** For example `yyyy-MM-dd HH:mm:ss.SSS`; the tokens are documented in `TimestampPatternTokens`. */
    val timestampPattern: String

    /** The zone the file's clock runs in, as typed; blank leaves it to the file. */
    val zoneId: String

    /**
     * A line described by literals and placeholders, for example `{timestamp} {level} {tag}: {message}`.
     *
     * Literal characters are matched verbatim and runs of whitespace match any whitespace.
     */
    data class Template(
        override val timestampPattern: String,
        val structureTemplate: String,
        override val zoneId: String = "",
    ) : ManualFormatInput

    /**
     * A line holding one JSON object, described by the keys its components live under.
     *
     * A key may be a dotted path (`log.level`), because structured loggers routinely nest.
     */
    data class Json(
        override val timestampPattern: String,
        val timestampKey: String,
        val levelKey: String = "",
        val tagKey: String = "",
        val messageKey: String = "",
        override val zoneId: String = "",
    ) : ManualFormatInput

    /**
     * A row of separated columns, each component addressed by header name or by position.
     *
     * A field written as `#2` means the third column; anything else is read as a header name, which
     * is only meaningful when [hasHeader] is set.
     */
    data class Delimited(
        override val timestampPattern: String,
        val delimiter: String,
        val hasHeader: Boolean,
        val timestampField: String,
        val levelField: String = "",
        val tagField: String = "",
        val messageField: String = "",
        override val zoneId: String = "",
    ) : ManualFormatInput
}
