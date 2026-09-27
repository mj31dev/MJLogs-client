package dev.mj31.logger.client.domain.format.spec

import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * Complete description of how one line of a log file is decomposed.
 *
 * The three variants differ only in how the components of a record are located within a line;
 * everything that happens afterwards — completing the timestamp, defaulting the level, assembling
 * the source — is shared, which is why those properties live on the interface. A parser is still
 * handed one line at a time whichever variant it was built from.
 *
 * [timestampPattern] describes the timestamp layout using the token subset documented in
 * [TimestampPatternTokens], and applies to all three: a JSON log carries its time as a string or an
 * epoch number just as a plain text one does.
 *
 * [zoneId] is the time zone the user said the file's clock runs in — an IANA name such as
 * `Europe/Berlin` or a fixed offset such as `UTC+03:00`. It is `null` when nobody said, which is
 * not the same as UTC: the file may still name its zone in its preamble, and only a zone the user
 * chose outranks that. An offset written into the timestamps themselves outranks both.
 */
sealed interface LogFormatSpec {

    val name: String
    val timestampPattern: String
    val fallbackLevel: LogLevel
    val zoneId: String?
    val origin: FormatOrigin

    /** The same layout read in another zone; `null` leaves the choice to the file. */
    fun withZoneId(zoneId: String?): LogFormatSpec = when (this) {
        is Regex -> copy(zoneId = zoneId)
        is Json -> copy(zoneId = zoneId)
        is Delimited -> copy(zoneId = zoneId)
    }

    /**
     * A line matched by a regular expression.
     *
     * [linePattern] must expose the named groups declared in [LogFormatGroups]; only
     * [LogFormatGroups.TIMESTAMP] is mandatory.
     */
    data class Regex(
        override val name: String,
        val linePattern: String,
        override val timestampPattern: String,
        override val fallbackLevel: LogLevel = LogLevel.INFO,
        override val zoneId: String? = null,
        override val origin: FormatOrigin = FormatOrigin.DETECTED,
    ) : LogFormatSpec

    /** A line holding one JSON object, whose components are read by key. */
    data class Json(
        override val name: String,
        val fields: RecordFieldMap,
        override val timestampPattern: String,
        override val fallbackLevel: LogLevel = LogLevel.INFO,
        override val zoneId: String? = null,
        override val origin: FormatOrigin = FormatOrigin.DETECTED,
    ) : LogFormatSpec

    /**
     * A row of delimiter separated columns.
     *
     * [hasHeader] tells the reader to skip the first row; it does not decide how columns are
     * located, because a file with a header may still be addressed by ordinal.
     */
    data class Delimited(
        override val name: String,
        val delimiter: Char,
        val hasHeader: Boolean,
        val fields: RecordFieldMap,
        override val timestampPattern: String,
        override val fallbackLevel: LogLevel = LogLevel.INFO,
        override val zoneId: String? = null,
        override val origin: FormatOrigin = FormatOrigin.DETECTED,
    ) : LogFormatSpec
}
