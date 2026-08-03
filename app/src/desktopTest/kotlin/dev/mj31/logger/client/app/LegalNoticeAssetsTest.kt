package dev.mj31.logger.client.app

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.Test

/**
 * The licence obligations are discharged by files, not by code: jpackage copies them into every
 * distribution and nothing else reads them, so a rename, a stale copy or a dependency bump would
 * only surface after a release. These assertions move that failure back into the suite.
 */
class LegalNoticeAssetsTest {

    @Test
    fun `every licence text the distribution promises is present`() {
        listOf("LICENSE.txt", "LGPL-3.0.txt", "GPL-3.0.txt", "THIRD-PARTY.txt").forEach { name ->
            val text = legalFile(name = name)

            assertThat(text.isFile).isTrue()
            assertThat(text.length()).isGreaterThan(1_000L)
        }
    }

    @Test
    fun `the shipped project licence is the one the repository declares`() {
        val shipped = legalFile(name = "LICENSE.txt").readText()
        val declared = File(repositoryRoot(), "LICENSE").readText()

        assertThat(shipped).isEqualTo(declared)
    }

    @Test
    fun `the copyleft texts are the ones the notice names`() {
        assertThat(legalFile(name = "LGPL-3.0.txt").readText())
            .contains("GNU LESSER GENERAL PUBLIC LICENSE")
        assertThat(legalFile(name = "GPL-3.0.txt").readText())
            .contains("GNU GENERAL PUBLIC LICENSE")
        assertThat(legalFile(name = "LICENSE.txt").readText())
            .contains("Apache License")
    }

    /**
     * The written offer of source code names one exact FFmpeg build. Bumping the dependency without
     * touching the notice would point users at sources that are not the ones they received.
     */
    @Test
    fun `the notice names the FFmpeg build that is actually bundled`() {
        val catalog = File(repositoryRoot(), "gradle/libs.versions.toml").readLines()
        val bundledVersion = catalog
            .first { it.trimStart().startsWith("ffmpeg =") }
            .substringAfter(delimiter = '"')
            .substringBefore(delimiter = '"')

        assertThat(legalFile(name = "THIRD-PARTY.txt").readText()).contains(bundledVersion)
    }

    private fun legalFile(name: String): File = File(moduleDirectory(), "legal/common/$name")

    /** Tests run from the module directory in Gradle and from the repository root in some IDEs. */
    private fun moduleDirectory(): File {
        val current = File(".").absoluteFile.normalize()
        return if (File(current, "legal").isDirectory) current else File(current, "app")
    }

    private fun repositoryRoot(): File = moduleDirectory().parentFile
}
