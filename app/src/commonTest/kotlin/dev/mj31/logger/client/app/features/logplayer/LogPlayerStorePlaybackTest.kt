package dev.mj31.logger.client.app.features.logplayer

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.app.view.text.UiText

class LogPlayerStorePlaybackTest {

    @Test
    fun `toggling playback starts a paused screencast`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.loadVideo()

        robot.dispatch(intent = LogPlayerIntent.TogglePlayback)

        assertThat(robot.player.playCallCount).isEqualTo(1)
        assertThat(robot.player.pauseCallCount).isEqualTo(0)
        assertThat(robot.state.video.isPlaying).isTrue()
    }

    @Test
    fun `toggling playback pauses a playing screencast`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.loadVideo()
        robot.dispatch(intent = LogPlayerIntent.TogglePlayback)

        robot.dispatch(intent = LogPlayerIntent.TogglePlayback)

        assertThat(robot.player.pauseCallCount).isEqualTo(1)
        assertThat(robot.state.video.isPlaying).isFalse()
    }

    @Test
    fun `seeking forwards the position to the player`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.loadVideo()

        robot.dispatch(intent = LogPlayerIntent.Seek(positionMillis = 12_345L))

        assertThat(robot.player.seekPositions).containsExactly(12_345L)
    }

    @Test
    fun `the playback state is mirrored into the screen state`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.loadVideo(durationMillis = 90_000L, positionMillis = 30_000L)

        val video = robot.state.video
        assertThat(video.name).isEqualTo(LogPlayerRobot.DEFAULT_VIDEO_NAME)
        assertThat(video.durationMillis).isEqualTo(90_000L)
        assertThat(video.positionMillis).isEqualTo(30_000L)
        assertThat(video.hasVideo).isTrue()
    }

    @Test
    fun `a playback error is surfaced instead of a video`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.loadVideo()

        robot.player.setStatus(status = PlaybackStatus.ERROR, errorMessage = "libVLC was not found")
        robot.settle()

        assertThat(robot.state.video.errorMessage).isEqualTo("libVLC was not found")
        assertThat(robot.state.video.hasVideo).isFalse()
    }

    @Test
    fun `decoded frames are exposed without going through the state`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        val frame = VideoFrame(width = 2, height = 2, pixels = ByteArray(size = 16), sequence = 1L)

        robot.player.videoFrames.value = frame
        robot.settle()

        assertThat(robot.store.frames.value).isSameInstanceAs(frame)
    }

    @Test
    fun `releasing the workspace releases the player`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.store.release()

        assertThat(robot.player.releaseCallCount).isEqualTo(1)
    }

    @Test
    fun `a file that is not a video is refused with an explanation`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importVideo(path = "/logs/app.txt")

        val refusal = robot.lastMessage as UiText.Raw
        assertThat(refusal.value).contains("app.txt")
        assertThat(refusal.value).contains(".mp4")
        assertThat(robot.player.openedMedia).isEmpty()
        assertThat(robot.state.video.name).isNull()
    }

    @Test
    fun `a log file of an unsupported type is refused by the import`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importLogFiles(paths = listOf("/media/screencast.mp4"))

        assertThat((robot.lastMessage as UiText.Raw).value).contains("screencast.mp4")
        assertThat(robot.state.sources).isEmpty()
        assertThat(robot.state.formatRequest).isNull()
    }

    @Test
    fun `an unsupported file does not stop the supported ones`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        robot.importLogFiles(paths = listOf("/media/photo.png", LogPlayerFixtures.FIRST_PATH))

        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME)
    }
}
