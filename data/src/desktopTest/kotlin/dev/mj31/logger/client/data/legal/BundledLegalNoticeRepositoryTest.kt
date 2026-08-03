package dev.mj31.logger.client.data.legal

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class BundledLegalNoticeRepositoryTest {

    @Test
    fun `reads every text file in the resources directory`() = runTest {
        val directory = directoryWith(
            "THIRD-PARTY.txt" to "bundled components",
            "LICENSE.txt" to "Apache License",
        )

        val notices = repositoryFor(directory = directory).read()

        assertThat(notices.map { it.fileName }).containsExactly("THIRD-PARTY.txt", "LICENSE.txt").inOrder()
        assertThat(notices.first().text).isEqualTo("bundled components")
    }

    @Test
    fun `the summary comes first and unknown texts come last`() = runTest {
        val directory = directoryWith(
            "GPL-3.0.txt" to "gpl",
            "ZLIB.txt" to "zlib",
            "LICENSE.txt" to "apache",
            "THIRD-PARTY.txt" to "summary",
            "LGPL-3.0.txt" to "lgpl",
        )

        val notices = repositoryFor(directory = directory).read()

        assertThat(notices.map { it.fileName })
            .containsExactly("THIRD-PARTY.txt", "LICENSE.txt", "LGPL-3.0.txt", "GPL-3.0.txt", "ZLIB.txt")
            .inOrder()
    }

    @Test
    fun `anything that is not a text file is ignored`() = runTest {
        val directory = directoryWith(
            "LICENSE.txt" to "apache",
            "icon.icns" to "not a licence",
        )

        val notices = repositoryFor(directory = directory).read()

        assertThat(notices.map { it.fileName }).containsExactly("LICENSE.txt")
    }

    @Test
    fun `an unpackaged run reports no notice rather than failing`() = runTest {
        assertThat(repositoryFor(directory = null).read()).isEmpty()
        assertThat(repositoryFor(directory = File("/no/such/directory")).read()).isEmpty()
    }

    private fun repositoryFor(directory: File?) = BundledLegalNoticeRepository(
        resourcesDirectory = directory,
        dispatcher = UnconfinedTestDispatcher(),
    )

    private fun directoryWith(vararg files: Pair<String, String>): File {
        val directory = createTempDirectory()
        files.forEach { (name, text) -> File(directory, name).writeText(text = text) }
        return directory
    }

    private fun createTempDirectory(): File =
        File.createTempFile("legal", "").let { file ->
            file.delete()
            file.mkdirs()
            file.deleteOnExit()
            file
        }
}
