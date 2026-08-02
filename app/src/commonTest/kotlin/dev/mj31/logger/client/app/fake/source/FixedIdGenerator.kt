package dev.mj31.logger.client.app.fake.source

import dev.mj31.logger.client.domain.source.IdGenerator

/**
 * [IdGenerator] handing out predictable identifiers so that entry ids stay assertable.
 *
 * With [ids] given, they are handed out in order and the last one repeats; otherwise the prefix
 * asked for is numbered, which is enough for a test that only cares about the ids being distinct.
 */
class FixedIdGenerator(
    private val ids: List<String> = emptyList(),
) : IdGenerator {

    private val mutableRequestedPrefixes = mutableListOf<String>()
    private var cursor = 0

    val requestedPrefixes: List<String>
        get() = mutableRequestedPrefixes.toList()

    override fun next(prefix: String): String {
        mutableRequestedPrefixes += prefix
        val id = ids.getOrNull(index = minOf(a = cursor, b = ids.lastIndex)) ?: "$prefix-${cursor + 1}"
        cursor++
        return id
    }
}
