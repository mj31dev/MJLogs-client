package dev.mj31.logger.client.data.source

import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads plain text log files from the local file system. */
class LocalTextFileDataSource(
    private val dispatcher: CoroutineDispatcher,
) : TextFileDataSource {

    override suspend fun read(path: String): TextFileContent = withContext(context = dispatcher) {
        val file = File(path)
        require(value = file.isFile) { "File not found: $path" }
        TextFileContent(
            path = file.absolutePath,
            name = file.name,
            lines = file.readLines(charset = Charsets.UTF_8),
        )
    }
}
