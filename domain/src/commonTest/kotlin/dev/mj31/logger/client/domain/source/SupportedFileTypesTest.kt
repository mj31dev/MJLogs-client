package dev.mj31.logger.client.domain.source

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class SupportedFileTypesTest {

    @Test
    fun `recognizes the accepted log extensions`() {
        listOf(
            "/logs/app.txt",
            "/logs/app.log",
            "C:\\logs\\app.LOG",
            "/logs/app.out",
            "/logs/app.err",
            "/logs/app.json",
            "/logs/app.jsonl",
            "/logs/app.ndjson",
            "/logs/app.csv",
            "/logs/app.tsv",
        ).forEach { path ->
            assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = path)).isTrue()
        }
    }

    @Test
    fun `recognizes containers as something a log can be taken from`() {
        listOf("/logs/app.log.gz", "/logs/bundle.zip", "/logs/app.txt.GZ").forEach { path ->
            assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = path)).isTrue()
            assertThat(SupportedFileTypes.isArchive(path = path)).isTrue()
        }
        assertThat(SupportedFileTypes.isArchive(path = "/logs/app.log")).isFalse()
    }

    @Test
    fun `recognizes what log rotation leaves behind`() {
        listOf("/logs/app.log.1", "/logs/app.log.2024-08-01", "/logs/app.log.1.gz").forEach { path ->
            assertThat(SupportedFileTypes.accepts(kind = MediaKind.LOG, path = path)).isTrue()
        }
    }

    @Test
    fun `does not let an arbitrary suffix turn another file into a log`() {
        // The rotation rule accepts a counter or a date, never a free suffix; a screencast whose name
        // merely contains `.log.` must stay a screencast.
        listOf("/logs/capture.log.mp4", "/logs/app.log.backup", "/logs/app", "/media/clip.mp4", "")
            .forEach { path ->
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
