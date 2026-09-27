package dev.mj31.logger.client.data.source

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LocalTextFileDataSourceTest {

    private val dataSource = LocalTextFileDataSource(dispatcher = Dispatchers.Unconfined)

    @Test
    fun `reads a file line by line`() = runTest {
        val file = temporaryFile(content = "first\nsecond\nthird")

        val content = dataSource.read(path = file.absolutePath)

        assertThat(content.lines).containsExactly("first", "second", "third").inOrder()
        assertThat(content.name).isEqualTo(file.name)
        assertThat(content.path).isEqualTo(file.absolutePath)
    }

    @Test
    fun `keeps blank lines so line numbers stay meaningful`() = runTest {
        val file = temporaryFile(content = "first\n\nthird")

        val content = dataSource.read(path = file.absolutePath)

        assertThat(content.lines).containsExactly("first", "", "third").inOrder()
    }

    @Test
    fun `reads non ascii content as utf-8`() = runTest {
        val file = temporaryFile(content = "первая строка\n第二行")

        val content = dataSource.read(path = file.absolutePath)

        assertThat(content.lines).containsExactly("первая строка", "第二行").inOrder()
    }

    @Test
    fun `an empty file yields no lines`() = runTest {
        val content = dataSource.read(path = temporaryFile(content = "").absolutePath)

        assertThat(content.lines).isEmpty()
    }

    @Test
    fun `a missing path fails with a readable message`() = runTest {
        val error = assertFailsWith<IllegalArgumentException> {
            dataSource.read(path = File(temporaryDirectory, "missing.txt").absolutePath)
        }

        assertThat(error.message).contains("missing.txt")
    }

    @Test
    fun `a directory is not readable as a file`() = runTest {
        assertFailsWith<IllegalArgumentException> { dataSource.read(path = temporaryDirectory.absolutePath) }
    }

    @Test
    fun `identifiers are unique and carry the requested prefix`() {
        val generator = UuidIdGenerator()

        val ids = List(size = 100) { generator.next(prefix = "src") }

        assertThat(ids.toSet()).hasSize(ids.size)
        assertThat(ids.all { it.startsWith(prefix = "src-") }).isTrue()
    }

    @Test
    fun `a UTF-8 byte order mark is not left at the head of the first record`() = runTest {
        val file = binaryFile(bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "first\nsecond".toByteArray(charset = Charsets.UTF_8))

        val content = dataSource.read(path = file.absolutePath)

        // Left in place the mark sits invisibly before the timestamp and stops the line matching.
        assertThat(content.lines).containsExactly("first", "second").inOrder()
    }

    @Test
    fun `a UTF-16 file is read through its byte order mark rather than as broken UTF-8`() = runTest {
        val file = binaryFile(bytes = "first\nsecond".toByteArray(charset = Charsets.UTF_16LE)
            .let { byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + it })

        val content = dataSource.read(path = file.absolutePath)

        assertThat(content.lines).containsExactly("first", "second").inOrder()
    }

    @Test
    fun `a file with no mark is still read as UTF-8`() = runTest {
        val file = temporaryFile(content = "привет\nmir")

        val content = dataSource.read(path = file.absolutePath)

        assertThat(content.lines).containsExactly("привет", "mir").inOrder()
    }

    private fun binaryFile(bytes: ByteArray): File =
        File.createTempFile("logger-client", ".log", temporaryDirectory).apply {
            writeBytes(array = bytes)
            deleteOnExit()
        }

    private fun temporaryFile(content: String): File =
        File.createTempFile("logger-client", ".txt", temporaryDirectory).apply {
            writeText(text = content, charset = Charsets.UTF_8)
            deleteOnExit()
        }

    private companion object {
        val temporaryDirectory: File = File(System.getProperty("java.io.tmpdir"))
    }
}
