package dev.mj31.logger.client.domain.format.compile

/** Input the user can act on to fix a rejected format. */
enum class FormatErrorField {
    TIMESTAMP_PATTERN,
    STRUCTURE_TEMPLATE,

    /** The failure belongs to neither input, for example when no line matches an otherwise valid format. */
    NONE,
}
