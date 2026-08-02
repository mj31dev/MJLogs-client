package dev.mj31.logger.client.app

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.app.view.text.UiText

/** Command line arguments are a second entry point into the workspace and follow the same rules. */
class StartupArgumentsTest {

    @Test
    fun `a screencast argument opens the player and the rest is imported as logs`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        openStartupFiles(paths = listOf(VIDEO_PATH, LogPlayerFixtures.FIRST_PATH), store = robot.store)
        robot.settle()

        assertThat(robot.player.openedMedia.map { it.path }).containsExactly(VIDEO_PATH)
        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME)
    }

    @Test
    fun `the order of the arguments does not matter`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        openStartupFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH, VIDEO_PATH), store = robot.store)
        robot.settle()

        assertThat(robot.player.openedMedia.map { it.path }).containsExactly(VIDEO_PATH)
        assertThat(robot.state.sources).hasSize(1)
    }

    @Test
    fun `only the first screencast is opened`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        openStartupFiles(paths = listOf(VIDEO_PATH, "/media/second.mov"), store = robot.store)
        robot.settle()

        assertThat(robot.player.openedMedia.map { it.path }).containsExactly(VIDEO_PATH)
    }

    @Test
    fun `an unsupported argument is reported instead of being silently dropped`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        openStartupFiles(paths = listOf("/media/photo.png"), store = robot.store)
        robot.settle()

        assertThat((robot.lastMessage as UiText.Raw).value).contains("photo.png")
        assertThat(robot.player.openedMedia).isEmpty()
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `no arguments leaves an empty workspace`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        openStartupFiles(paths = emptyList(), store = robot.store)
        robot.settle()

        assertThat(robot.state.hasLogs).isFalse()
        assertThat(robot.player.openedMedia).isEmpty()
        assertThat(robot.effects).isEmpty()
    }

    private companion object {
        const val VIDEO_PATH = "/media/screencast.mp4"
    }
}
