package dev.mj31.logger.client.domain.source

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class SupportedFileTypesTest {

    @Test
    fun `recognizes the accepted log extensions`() {
        assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = "/logs/app.txt")).isTrue()
        assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = "/logs/app.log")).isTrue()
        assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = "C:\\logs\\app.LOG")).isTrue()
    }

    @Test
    fun `rejects anything else as a log`() {
        listOf("/logs/app.txt.gz", "/logs/app", "/logs/app.csv", "/media/clip.mp4", "").forEach { path ->
            assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = path)).isFalse()
        }
    }

    @Test
    fun `recognizes the accepted video extensions`() {
        listOf("/media/clip.mp4", "/media/clip.MOV", "/media/clip.mkv", "/media/clip.webm").forEach { path ->
            assertThat(SupportedFileTypes.accepts(kind = MediaKind.VIDEO, path = path)).isTrue()
        }
    }

    @Test
    fun `rejects a log file as a video and the other way round`() {
        assertThat(SupportedFileTypes.accepts(kind = MediaKind.VIDEO, path = "/logs/app.txt")).isFalse()
        assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = "/media/clip.mp4")).isFalse()
    }

    @Test
    fun `classifies a path into a single kind`() {
        assertThat(SupportedFileTypes.kindOf(path = "/logs/app.txt")).isEqualTo(MediaKind.LOG)
        assertThat(SupportedFileTypes.kindOf(path = "/media/clip.mp4")).isEqualTo(MediaKind.VIDEO)
        assertThat(SupportedFileTypes.kindOf(path = "/media/photo.png")).isNull()
    }

    @Test
    fun `the two kinds never share an extension`() {
        val shared = SupportedFileTypes.logExtensions intersect SupportedFileTypes.videoExtensions

        assertThat(shared).isEmpty()
    }

    @Test
    fun `every extension is declared in lower case with a leading dot`() {
        val all = SupportedFileTypes.logExtensions + SupportedFileTypes.videoExtensions

        assertThat(all.all { it.startsWith(prefix = ".") && it == it.lowercase() }).isTrue()
    }

    @Test
    fun `the rejection message names the file and the accepted types`() {
        val message = SupportedFileTypes.rejectionMessage(kind = MediaKind.LOG, fileName = "photo.png")

        assertThat(message).contains("photo.png")
        assertThat(message).contains(".txt")
        assertThat(message).contains(".log")
    }
}
