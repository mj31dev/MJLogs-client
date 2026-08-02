package dev.mj31.logger.client.app.fake.source

import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource

/**
 * In-memory [TextFileDataSource] serving registered content and registered read failures.
 *
 * Content can be handed over at construction time, which reads well for a single-file test, or
 * registered later, which is what a test driving several imports through the UI needs.
 */
class FakeTextFileDataSource(
    contentByPath: Map<String, TextFileContent> = emptyMap(),
    errorByPath: Map<String, Throwable> = emptyMap(),
) : TextFileDataSource {

    private val contents = contentByPath.toMutableMap()
    private val errors = errorByPath.toMutableMap()
    private val mutableRequestedPaths = mutableListOf<String>()

    val requestedPaths: List<String>
        get() = mutableRequestedPaths.toList()

    fun register(content: TextFileContent) {
        contents[content.path] = content
    }

    /** Makes every read of [path] fail with [error], simulating a missing or unreadable file. */
    fun registerFailure(path: String, error: Throwable) {
        errors[path] = error
    }

    override suspend fun read(path: String): TextFileContent {
        mutableRequestedPaths += path
        errors[path]?.let { error -> throw error }
        return contents[path] ?: throw IllegalStateException("No content registered for $path")
    }

    companion object {

        /** Convenience factory for the common "one readable file" setup. */
        fun of(content: TextFileContent): FakeTextFileDataSource =
            FakeTextFileDataSource(contentByPath = mapOf(content.path to content))

        /** Convenience factory for a path whose read always fails with [error]. */
        fun failing(path: String, error: Throwable): FakeTextFileDataSource =
            FakeTextFileDataSource(errorByPath = mapOf(path to error))
    }
}
