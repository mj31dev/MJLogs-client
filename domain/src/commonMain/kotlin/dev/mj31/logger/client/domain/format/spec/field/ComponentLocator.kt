package dev.mj31.logger.client.domain.format.spec.field

/**
 * Where one component of a record is found inside a structured line.
 *
 * A regular expression locates a component by named group, which is why this type is irrelevant to
 * [dev.mj31.logger.client.domain.format.spec.LogFormatSpec.Regex]. A JSON object locates it by key
 * and a delimited row by either a header name or an ordinal, so both forms are needed here rather
 * than one per format.
 */
sealed interface ComponentLocator {

    /** The name of a JSON key, or of a column when the file carries a header row. */
    data class Key(val name: String) : ComponentLocator

    /** The zero based position of a column, used when a delimited file has no header row. */
    data class Index(val position: Int) : ComponentLocator
}
