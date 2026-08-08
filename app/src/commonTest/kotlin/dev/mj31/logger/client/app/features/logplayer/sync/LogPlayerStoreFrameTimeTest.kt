package dev.mj31.logger.client.app.features.logplayer.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_load_screencast_first
import dev.mj31.logger.client.app.view.text.UiText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * Synchronizing on a time read off the frame itself.
 *
 * The fixture session starts at `2024-05-01T10:00:00Z`, so `10:00:20` is the record at +20s of the
 * log timeline even though no record has to be selected for this path to work.
 */
class LogPlayerStoreFrameTimeTest {

    @Test
    fun `a typed frame time synchronizes without any record being selected`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)

        robot.typeFrameTime(text = "10:00:20")
        robot.synchronizeAtFrameTime()

        assertThat(robot.state.sync.isSynced).isTrue()
        assertThat(robot.state.sync.anchorEntryId).isNull()
        assertThat(robot.state.sync.anchorVideoPositionMillis).isEqualTo(10_000L)
        assertThat(robot.state.selectedEntryId).isNull()
    }

    @Test
    fun `the frame time maps the playhead onto the log timeline`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)

        robot.typeFrameTime(text = "10:00:20")
        robot.synchronizeAtFrameTime()
        robot.movePlayheadTo(positionMillis = 30_000L)

        assertThat(robot.state.sync.logTimeAtPlayhead).isEqualTo(LogPlayerFixtures.at(offsetMillis = 40_000L))
        assertThat(robot.state.activeEntryId).isEqualTo(LogPlayerFixtures.FIFTH_ENTRY_ID)
    }

    @Test
    fun `a full date and time is accepted too`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 5_000L)

        robot.typeFrameTime(text = "2024-05-01 10:00:20.000")
        robot.synchronizeAtFrameTime()

        assertThat(robot.state.sync.isSynced).isTrue()
        assertThat(robot.state.sync.frameTimeError).isFalse()
    }

    @Test
    fun `an unreadable time is reported on the field itself`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        val messagesBefore = robot.messages

        robot.typeFrameTime(text = "at some point")
        robot.synchronizeAtFrameTime()

        assertThat(robot.state.sync.frameTimeError).isTrue()
        assertThat(robot.state.sync.isSynced).isFalse()
        // The complaint belongs to the field, so no transient notice is raised on top of it.
        assertThat(robot.messages).isEqualTo(messagesBefore)
    }

    @Test
    fun `editing the field clears the previous complaint`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()
        robot.typeFrameTime(text = "at some point")
        robot.synchronizeAtFrameTime()

        robot.typeFrameTime(text = "10:00:20")

        assertThat(robot.state.sync.frameTimeError).isFalse()
    }

    @Test
    fun `without a screencast the frame time explains what is missing`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.typeFrameTime(text = "10:00:20")
        robot.synchronizeAtFrameTime()

        assertThat(robot.lastMessage)
            .isEqualTo(UiText.Resource(resource = Res.string.message_load_screencast_first))
        assertThat(robot.state.sync.isSynced).isFalse()
    }

    @Test
    fun `the action stays disabled while the field is empty`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        assertThat(robot.state.sync.canSynchronizeAtFrameTime).isFalse()

        robot.typeFrameTime(text = "10:00:20")

        assertThat(robot.state.sync.canSynchronizeAtFrameTime).isTrue()
    }

    @Test
    fun `a frame time replaces an anchor made from a record`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)
        robot.selectEntry(entryId = LogPlayerFixtures.FIRST_ENTRY_ID)
        robot.synchronize()

        robot.typeFrameTime(text = "10:00:20")
        robot.synchronizeAtFrameTime()

        assertThat(robot.state.sync.anchorEntryId).isNull()
        assertThat(robot.state.sync.isSynced).isTrue()
    }

    @Test
    fun `a picked date and time fill the field`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        robot.pickFrameTime(dateMillis = MAY_FIRST_MILLIS, hour = 10, minute = 0)

        assertThat(robot.state.sync.frameTime).isEqualTo("2024-05-01 10:00:00.000")
        assertThat(robot.state.sync.canSynchronizeAtFrameTime).isTrue()
    }

    @Test
    fun `picking keeps the seconds already typed`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()
        robot.typeFrameTime(text = "12:00:20.500")

        robot.pickFrameTime(dateMillis = MAY_FIRST_MILLIS, hour = 10, minute = 0)

        assertThat(robot.state.sync.frameTime).isEqualTo("2024-05-01 10:00:20.500")
    }

    @Test
    fun `a picked time can be applied straight away`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)

        robot.pickFrameTime(dateMillis = MAY_FIRST_MILLIS, hour = 10, minute = 0)
        robot.synchronizeAtFrameTime()

        assertThat(robot.state.sync.isSynced).isTrue()
        assertThat(robot.state.sync.logTimeAtPlayhead).isEqualTo(LogPlayerFixtures.at(offsetMillis = 0L))
    }

    @Test
    fun `picking clears a previous complaint about the field`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()
        robot.typeFrameTime(text = "at some point")
        robot.synchronizeAtFrameTime()

        robot.pickFrameTime(dateMillis = MAY_FIRST_MILLIS, hour = 10, minute = 0)

        assertThat(robot.state.sync.frameTimeError).isFalse()
    }

    @Test
    fun `the picker opens on the session start while the field is empty`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        assertThat(robot.state.sync.frameTimeDefault).isEqualTo(LogPlayerFixtures.at(offsetMillis = 0L))
    }

    @Test
    fun `the picker opens on the typed time once it can be read`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        robot.typeFrameTime(text = "10:00:40")

        assertThat(robot.state.sync.frameTimeDefault).isEqualTo(LogPlayerFixtures.at(offsetMillis = 40_000L))
    }

    private companion object {
        /** Midnight UTC of the fixture session day, which is what a date picker reports. */
        val MAY_FIRST_MILLIS: Long = Instant.parse("2024-05-01T00:00:00Z").toEpochMilliseconds()
    }
}
