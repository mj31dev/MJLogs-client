package dev.mj31.logger.client.domain.format

/** Component of a record a fragment of a raw line belongs to. */
enum class LogComponent {
    TIMESTAMP,
    LEVEL,
    TAG,
    MESSAGE,
}
