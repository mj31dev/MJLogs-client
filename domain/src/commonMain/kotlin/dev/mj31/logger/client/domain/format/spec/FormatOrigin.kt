package dev.mj31.logger.client.domain.format.spec

/** Where a [LogFormatSpec] came from; shown to the user and used to prefer detected specs on reuse. */
enum class FormatOrigin {
    DETECTED,
    USER_DEFINED,
}
