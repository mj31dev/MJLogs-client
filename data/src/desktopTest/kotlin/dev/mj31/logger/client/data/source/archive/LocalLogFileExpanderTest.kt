package dev.mj31.logger.client.data.source.archive

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class LocalLogFileExpanderTest {

    private val cache = File(System.getProperty("java.io.tmpdir"), "mjlogs-expander-test").also {
        it.deleteRecursively()
        it.mkdirs()
    }

    private val expander = LocalLogFileExpander(cacheDirectory = cache, dispatcher = Dispatchers.Unconfined)

    @Test
    fun `an ordinary file denotes itself`() = runTest {
        val file = write(name = "app.log", content = "first\nsecond")

        val expanded = expander.expand(path = file.absolutePath).single()

        assertThat(expanded.path).isEqualTo(file.absolutePath)
        assertThat(expanded.displayName).isEqualTo("app.log")
        assertThat(expanded.modifiedAt).isNotNull()
    }

    @Test
    fun `a gzip file denotes its content under the name without the suffix`() = runTest {
        val source = File(directory, "app.log.gz")
        GZIPOutputStream(source.outputStream()).use { it.write("hello".toByteArray()) }

        val expanded = expander.expand(path = source.absolutePath).single()

        assertThat(expanded.displayName).isEqualTo("app.log")
        assertThat(File(expanded.path).readText()).isEqualTo("hello")
    }

    @Test
    fun `every log inside an archive becomes a file of its own`() = runTest {
        val archive = zip(
            name = "bundle.zip",
            entries = mapOf(
                "logs/app.log" to "one",
                "logs/network.txt" to "two",
                "readme.md" to "ignored",
                "clip.mp4" to "ignored",
            ),
        )

        val expanded = expander.expand(path = archive.absolutePath)

        assertThat(expanded.map { it.displayName }).containsExactly("app.log", "network.txt")
        assertThat(expanded.map { File(it.path).readText() }).containsExactly("one", "two")
    }

    @Test
    fun `an entry whose name climbs out of the staging directory is refused`() = runTest {
        // Entry names come from a file someone else made; `../` in one of them would otherwise write
        // wherever this process can reach.
        val archive = zip(name = "evil.zip", entries = mapOf("../escaped.log" to "payload", "safe.log" to "kept"))

        val expanded = expander.expand(path = archive.absolutePath)

        assertThat(expanded.map { it.displayName }).containsExactly("safe.log")
        assertThat(File(cache.parentFile, "escaped.log").exists()).isFalse()
    }

    @Test
    fun `an archive holding nothing readable expands to nothing rather than failing`() = runTest {
        val archive = zip(name = "empty.zip", entries = mapOf("notes.md" to "text"))

        assertThat(expander.expand(path = archive.absolutePath)).isEmpty()
    }

    private fun write(name: String, content: String): File =
        File(directory, name).apply { writeText(text = content) }

    private fun zip(name: String, entries: Map<String, String>): File {
        val archive = File(directory, name)
        ZipOutputStream(archive.outputStream()).use { out ->
            entries.forEach { (entryName, content) ->
                out.putNextEntry(ZipEntry(entryName))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return archive
    }

    private companion object {
        val directory: File = File(System.getProperty("java.io.tmpdir"), "mjlogs-expander-input").also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }
}
