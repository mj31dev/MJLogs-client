package dev.mj31.logger.client.domain.source

/** Port for generating stable identifiers without pulling a platform UUID API into the domain. */
interface IdGenerator {
    fun next(prefix: String): String
}
