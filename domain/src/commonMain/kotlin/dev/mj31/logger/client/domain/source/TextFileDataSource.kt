package dev.mj31.logger.client.domain.source

/** Port for reading text files; the platform specific implementation lives in the data layer. */
interface TextFileDataSource {
    suspend fun read(path: String): TextFileContent
}
