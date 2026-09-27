package dev.mj31.logger.client.domain.format.compile

/**
 * Input the user can act on to fix a rejected format.
 *
 * Which of these can appear depends on the shape being described: a template has no delimiter, and a
 * JSON object has no structure template. The dialog shows one set of inputs at a time and marks the
 * one named here.
 */
enum class FormatErrorField {
    TIMESTAMP_PATTERN,
    STRUCTURE_TEMPLATE,
    DELIMITER,
    TIMESTAMP_FIELD,
    LEVEL_FIELD,
    TAG_FIELD,
    MESSAGE_FIELD,
    ZONE,

    /** The failure belongs to no single input, for example when no line matches an otherwise valid format. */
    NONE,
}
