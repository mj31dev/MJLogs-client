package dev.mj31.logger.client.data.source

import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads plain text log files from the local file system.
 *
 * The encoding is taken from the byte order mark and defaults to UTF-8. Guessing beyond that — a
 * cp1251 or cp1252 file carries no mark at all — needs statistics over the whole content and gets
 * it wrong often enough to be worse than a stated default, so it is deliberately not attempted.
 */
class LocalTextFileDataSource(
    private val dispatcher: CoroutineDispatcher,
) : TextFileDataSource {

    override suspend fun read(path: String): TextFileContent = withContext(context = dispatcher) {
        val file = File(path)
        require(value = file.isFile) { "File not found: $path" }
        val lines = file.reader(charset = charsetOf(file = file)).buffered().readLines()
        TextFileContent(
            path = file.absolutePath,
            name = file.name,
            lines = withoutByteOrderMark(lines = lines),
        )
    }

    private fun charsetOf(file: File): Charset {
        val prefix = ByteArray(size = BOM_LENGTH)
        val read = file.inputStream().use { stream -> stream.read(prefix) }
        return when {
            read >= UTF8_BOM.size && prefix.startsWith(prefix = UTF8_BOM) -> Charsets.UTF_8
            read >= UTF16_LE_BOM.size && prefix.startsWith(prefix = UTF16_LE_BOM) -> Charsets.UTF_16LE
            read >= UTF16_BE_BOM.size && prefix.startsWith(prefix = UTF16_BE_BOM) -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
    }

    /**
     * A decoder hands the mark over as an ordinary character, which would otherwise sit invisibly at
     * the head of the first record and stop it from matching any format.
     */
    private fun withoutByteOrderMark(lines: List<String>): List<String> {
        val first = lines.firstOrNull() ?: return lines
        if (!first.startsWith(prefix = BOM_CHARACTER)) return lines
        return lines.toMutableList().also { it[0] = first.removePrefix(prefix = BOM_CHARACTER) }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        prefix.indices.all { index -> this[index] == prefix[index] }

    private companion object {
        const val BOM_LENGTH = 3
        const val BOM_CHARACTER = "\uFEFF"
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    }
}
