package dev.mj31.logger.client.data.source

import dev.mj31.logger.client.domain.source.IdGenerator
import java.util.UUID

/** Random identifier generator backed by the JDK. */
class UuidIdGenerator : IdGenerator {

    override fun next(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(n = 8)}"
}
