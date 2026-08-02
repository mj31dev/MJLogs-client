package dev.mj31.logger.client.app.platform

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.source.MediaKind
import java.io.File
import kotlin.test.Test

/**
 * The dialog itself cannot be opened headlessly, but what it is told to accept can be checked.
 */
class FileChooserFilterTest {

    private val directory = File(System.getProperty("java.io.tmpdir"))

    @Test
    fun `the log filter accepts log files only`() {
        val filter = filenameFilterFor(kind = MediaKind.LOG)

        assertThat(filter.accept(directory, "app.txt")).isTrue()
        assertThat(filter.accept(directory, "app.log")).isTrue()
        assertThat(filter.accept(directory, "APP.LOG")).isTrue()
        assertThat(filter.accept(directory, "clip.mp4")).isFalse()
        assertThat(filter.accept(directory, "photo.png")).isFalse()
        assertThat(filter.accept(directory, "app")).isFalse()
    }

    @Test
    fun `the video filter accepts screencasts only`() {
        val filter = filenameFilterFor(kind = MediaKind.VIDEO)

        assertThat(filter.accept(directory, "clip.mp4")).isTrue()
        assertThat(filter.accept(directory, "clip.MOV")).isTrue()
        assertThat(filter.accept(directory, "app.txt")).isFalse()
    }

    @Test
    fun `the windows glob lists every accepted extension`() {
        val glob = globFor(kind = MediaKind.LOG)

        assertThat(glob).contains("*.txt")
        assertThat(glob).contains("*.log")
        assertThat(glob.split(";").size).isEqualTo(2)
    }
}
