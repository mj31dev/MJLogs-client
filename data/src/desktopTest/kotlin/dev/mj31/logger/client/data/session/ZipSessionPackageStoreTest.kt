package dev.mj31.logger.client.data.session

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

/** Session files, written to and read from a real archive on a real disk. */
class ZipSessionPackageStoreTest {

    private val root = Files.createTempDirectory("mjlogs-package").toFile()
    private val store = ZipSessionPackageStore(
        cacheDirectory = root.resolve("cache"),
        dispatcher = Dispatchers.IO,
    )

    private val logFile = file(name = "network.txt", content = "10:00:00 INFO Boot started")
    private val videoFile = file(name = "clip.mp4", content = "not really a video, but real bytes")

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `a session file carries the files and reopens pointing at the copies`() = runTest {
        val target = root.resolve("investigation.mjclog").absolutePath

        store.write(targetPath = target, snapshot = snapshot()).toList()
        val reopened = store.read(path = target)

        val restoredLog = File(reopened.snapshot.logSources.single().path)
        assertThat(restoredLog.absolutePath).isNotEqualTo(logFile.absolutePath)
        assertThat(restoredLog.readText()).isEqualTo(logFile.readText())
        assertThat(File(reopened.snapshot.video!!.path).readText()).isEqualTo(videoFile.readText())
    }

    /** Self-contained is the whole point: the archive has to outweigh what it was made from. */
    @Test
    fun `the file is at least as large as what it bundles`() = runTest {
        val target = root.resolve("weight.mjclog").absolutePath

        store.write(targetPath = target, snapshot = snapshot()).toList()

        assertThat(File(target).length()).isAtLeast(logFile.length() + videoFile.length())
    }

    @Test
    fun `everything else survives the round trip`() = runTest {
        val target = root.resolve("round.mjclog").absolutePath
        val original = snapshot().copy(
            filter = LogFilter(query = "timeout", levels = setOf(LogLevel.ERROR)),
            timeWindowMillis = 4_000L,
            followVideo = false,
            videoPositionMillis = 77_000L,
        )

        store.write(targetPath = target, snapshot = original).toList()
        val reopened = store.read(path = target).snapshot

        assertThat(reopened.filter).isEqualTo(original.filter)
        assertThat(reopened.timeWindowMillis).isEqualTo(original.timeWindowMillis)
        assertThat(reopened.followVideo).isFalse()
        assertThat(reopened.videoPositionMillis).isEqualTo(77_000L)
        assertThat(reopened.packagePath).isEqualTo(target)
    }

    @Test
    fun `updating the workspace leaves the bundled files alone`() = runTest {
        val target = root.resolve("update.mjclog").absolutePath
        store.write(targetPath = target, snapshot = snapshot()).toList()
        val opened = store.read(path = target)

        store.updateSnapshot(
            path = target,
            snapshot = opened.snapshot.copy(filter = LogFilter(query = "changed")),
        )
        val reopened = store.read(path = target)

        assertThat(reopened.snapshot.filter.query).isEqualTo("changed")
        assertThat(File(reopened.snapshot.video!!.path).readText()).isEqualTo(videoFile.readText())
    }

    @Test
    fun `a cancelled write leaves nothing behind`() = runTest {
        val target = root.resolve("cancelled.mjclog").absolutePath
        // Large enough that the copy cannot finish while the collector is still taking its first
        // values: with a small file the write completes first and the test would prove nothing.
        val big = root.resolve("big.mp4").apply { writeBytes(ByteArray(size = BIG_FILE_BYTES)) }
        val heavy = snapshot().copy(video = VideoMedia(path = big.absolutePath, name = big.name))

        // Stopping the collection is exactly what pressing cancel does. Nothing may survive it:
        // a half-written archive is not a session, and it would sit next to the real ones.
        store.write(targetPath = target, snapshot = heavy).take(count = 2).toList()

        assertThat(File(target).exists()).isFalse()
        assertThat(File("$target${SessionPackageLayout.PARTIAL_SUFFIX}").exists()).isFalse()
    }

    @Test
    fun `progress is reported against the bundled bytes`() = runTest {
        val target = root.resolve("progress.mjclog").absolutePath

        val reported = store.write(targetPath = target, snapshot = snapshot()).toList()

        val expected = logFile.length() + videoFile.length()
        assertThat(reported.first().copiedBytes).isEqualTo(0L)
        assertThat(reported.last().copiedBytes).isEqualTo(expected)
        assertThat(reported.last().fraction).isEqualTo(1f)
    }

    @Test
    fun `releasing the cache removes what was unpacked`() = runTest {
        val target = root.resolve("release.mjclog").absolutePath
        store.write(targetPath = target, snapshot = snapshot()).toList()
        val extracted = File(store.read(path = target).snapshot.logSources.single().path)

        store.releaseExtracted(path = target)

        assertThat(extracted.exists()).isFalse()
    }

    private fun snapshot(): WorkspaceSnapshot = WorkspaceSnapshot(
        logSources = listOf(
            LogSourceRef(
                id = "src-1",
                name = logFile.name,
                path = logFile.absolutePath,
                format = LogFormatSpec(
                    name = "time seconds",
                    linePattern = "^(?<ts>.*)$",
                    timestampPattern = "HH:mm:ss",
                ),
            ),
        ),
        video = VideoMedia(path = videoFile.absolutePath, name = videoFile.name),
    )

    private fun file(name: String, content: String): File =
        root.resolve(name).apply { parentFile.mkdirs(); writeText(content) }

    private companion object {
        /** Several hundred copy iterations, so cancelling lands in the middle of one. */
        const val BIG_FILE_BYTES = 16 * 1024 * 1024
    }
}
