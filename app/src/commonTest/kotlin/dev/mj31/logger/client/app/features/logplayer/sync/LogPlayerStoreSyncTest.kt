package dev.mj31.logger.client.app.features.logplayer.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_load_screencast_first
import dev.mj31.logger.client.app.resources.message_record_outside_video
import dev.mj31.logger.client.app.resources.message_select_record_first

/**
 * The anchor used by most tests pins the record at +20s of the log timeline to 00:10 of the
 * screencast, so the recording starts at +10s of the log timeline and ends at +70s.
 */
class LogPlayerStoreSyncTest {

    @Test
    fun `before synchronization the timelines are independent`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 0L)

        robot.movePlayheadTo(positionMillis = 25_000L)
        robot.selectEntry(entryId = LogPlayerFixtures.FIFTH_ENTRY_ID)

        assertThat(robot.state.sync.isSynced).isFalse()
        assertThat(robot.state.activeEntryId).isNull()
        assertThat(robot.player.seekPositions).isEmpty()
    }

    @Test
    fun `synchronizing without a selected record explains what is missing`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo()

        robot.synchronize()

        assertThat(robot.lastMessage)
            .isEqualTo(UiText.Resource(resource = Res.string.message_select_record_first))
        assertThat(robot.state.sync.isSynced).isFalse()
    }

    @Test
    fun `synchronizing without a screencast explains what is missing`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.selectEntry(entryId = LogPlayerFixtures.FIRST_ENTRY_ID)

        robot.synchronize()

        assertThat(robot.lastMessage)
            .isEqualTo(UiText.Resource(resource = Res.string.message_load_screencast_first))
        assertThat(robot.state.sync.isSynced).isFalse()
        assertThat(robot.state.sync.canSynchronize).isFalse()
    }

    @Test
    fun `synchronizing pins the selected record to the playhead`() = runTest {
        val robot = syncedRobot()

        val sync = robot.state.sync
        assertThat(sync.isSynced).isTrue()
        assertThat(sync.anchorEntryId).isEqualTo(LogPlayerFixtures.THIRD_ENTRY_ID)
        assertThat(sync.anchorVideoPositionMillis).isEqualTo(ANCHOR_VIDEO_POSITION)
        assertThat(robot.syncRepository.syncState.value.anchorOrNull?.logTimestamp)
            .isEqualTo(LogPlayerFixtures.at(offsetMillis = 20_000L))
    }

    @Test
    fun `after synchronization the playhead drives the active record`() = runTest {
        val robot = syncedRobot()

        robot.movePlayheadTo(positionMillis = 25_000L)

        // 00:25 of the video maps to +35s of the log timeline: the newest record not after it is +30s.
        assertThat(robot.state.activeEntryId).isEqualTo(LogPlayerFixtures.FOURTH_ENTRY_ID)

        robot.movePlayheadTo(positionMillis = 0L)

        // The recording starts at +10s of the log timeline, where the second record sits.
        assertThat(robot.state.activeEntryId).isEqualTo(LogPlayerFixtures.SECOND_ENTRY_ID)
    }

    @Test
    fun `after synchronization selecting a record moves the video`() = runTest {
        val robot = syncedRobot()

        robot.selectEntry(entryId = LogPlayerFixtures.FIFTH_ENTRY_ID)

        // The record at +40s of the log timeline sits at 00:30 of the screencast.
        assertThat(robot.player.seekPositions).containsExactly(30_000L)
        assertThat(robot.state.selectedEntryId).isEqualTo(LogPlayerFixtures.FIFTH_ENTRY_ID)
    }

    @Test
    fun `a record outside the recording cannot be jumped to`() = runTest {
        val robot = syncedRobot()

        robot.selectEntry(entryId = LogPlayerFixtures.FIRST_ENTRY_ID)

        assertThat(robot.player.seekPositions).isEmpty()
        assertThat(robot.lastMessage)
            .isEqualTo(UiText.Resource(resource = Res.string.message_record_outside_video))
    }

    @Test
    fun `disabling follow video keeps the list from moving the player`() = runTest {
        val robot = syncedRobot()

        robot.setFollowVideo(enabled = false)
        robot.selectEntry(entryId = LogPlayerFixtures.FIFTH_ENTRY_ID)

        assertThat(robot.player.seekPositions).isEmpty()
        assertThat(robot.state.followVideo).isFalse()
    }

    @Test
    fun `unlinking detaches the timelines again`() = runTest {
        val robot = syncedRobot()
        robot.movePlayheadTo(positionMillis = 25_000L)
        assertThat(robot.state.activeEntryId).isNotNull()

        robot.clearSynchronization()

        assertThat(robot.state.sync.isSynced).isFalse()
        assertThat(robot.state.activeEntryId).isNull()
        assertThat(robot.state.sync.overlap?.hasOverlap).isFalse()
    }

    @Test
    fun `the sync bar reports how much of the session the recording covers`() = runTest {
        val robot = syncedRobot()

        val overlap = robot.state.sync.overlap
        assertThat(overlap?.hasOverlap).isTrue()
        // Recording spans +10s..+70s, the session spans 0s..+40s.
        assertThat(overlap?.overlap?.start).isEqualTo(LogPlayerFixtures.at(offsetMillis = 10_000L))
        assertThat(overlap?.overlap?.end).isEqualTo(LogPlayerFixtures.at(offsetMillis = 40_000L))
    }

    @Test
    fun `a recording that ends before the session starts is reported as disjoint`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        // Pinning the record at +20s to 01:00 of the screencast places the recording start at -40s.
        robot.loadVideo(positionMillis = 60_000L)
        robot.selectEntry(entryId = LogPlayerFixtures.THIRD_ENTRY_ID)
        robot.synchronize()
        assertThat(robot.state.sync.overlap?.hasOverlap).isTrue()

        // The real duration turns out to be 5s, so the recording spans -40s..-35s of the session.
        robot.player.setDuration(durationMillis = 5_000L)
        robot.settle()

        assertThat(robot.state.sync.overlap?.hasOverlap).isFalse()
        assertThat(robot.state.sync.overlap?.videoRange?.end)
            .isEqualTo(LogPlayerFixtures.at(offsetMillis = -35_000L))
    }

    private fun TestScope.syncedRobot(): LogPlayerRobot {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = ANCHOR_VIDEO_POSITION)
        robot.selectEntry(entryId = LogPlayerFixtures.THIRD_ENTRY_ID)
        robot.synchronize()
        return robot
    }

    private companion object {
        const val ANCHOR_VIDEO_POSITION = 10_000L
    }
}
