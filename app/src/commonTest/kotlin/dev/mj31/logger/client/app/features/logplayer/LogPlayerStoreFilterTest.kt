package dev.mj31.logger.client.app.features.logplayer

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogFilter

class LogPlayerStoreFilterTest {

    @Test
    fun `filtering by level hides the other records but keeps the total count`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.updateFilter(filter = LogFilter(levels = setOf(LogLevel.ERROR)))

        assertThat(robot.state.entries.map { it.id }).containsExactly(LogPlayerFixtures.FIFTH_ENTRY_ID)
        assertThat(robot.state.totalEntryCount).isEqualTo(LogPlayerFixtures.mergedEntryIds.size)
        assertThat(robot.state.isFiltered).isTrue()
    }

    @Test
    fun `filtering by free text matches tag and message`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.updateFilter(filter = LogFilter(query = "storage"))

        assertThat(robot.state.entries.map { it.sourceId })
            .containsExactly(LogPlayerFixtures.SECOND_SOURCE_ID, LogPlayerFixtures.SECOND_SOURCE_ID)

        robot.updateFilter(filter = LogFilter(query = "handshake"))

        assertThat(robot.state.entries.map { it.id }).containsExactly(LogPlayerFixtures.THIRD_ENTRY_ID)
    }

    @Test
    fun `filtering by source keeps only that file`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.updateFilter(filter = LogFilter(sourceIds = setOf(LogPlayerFixtures.SECOND_SOURCE_ID)))

        assertThat(robot.state.entries.map { it.id })
            .containsExactly(LogPlayerFixtures.SECOND_ENTRY_ID, LogPlayerFixtures.FOURTH_ENTRY_ID)
            .inOrder()
        assertThat(robot.state.sources.map { it.isSelected }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `empty criteria mean no restriction`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.updateFilter(filter = LogFilter(levels = emptySet(), sourceIds = emptySet(), query = "  "))

        assertThat(robot.state.entries).hasSize(LogPlayerFixtures.mergedEntryIds.size)
        assertThat(robot.state.isFiltered).isFalse()
    }

    @Test
    fun `the time window has no effect while the timelines are independent`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 0L)

        robot.setTimeWindow(windowMillis = 5_000L)

        assertThat(robot.state.entries).hasSize(LogPlayerFixtures.mergedEntryIds.size)
        assertThat(robot.state.timeWindowMillis).isEqualTo(5_000L)
    }

    @Test
    fun `once synchronized the time window follows the playhead`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)
        // The record at +20s of the log timeline happens at 00:10 of the screencast.
        robot.selectEntry(entryId = LogPlayerFixtures.THIRD_ENTRY_ID)
        robot.synchronize()

        robot.setTimeWindow(windowMillis = 5_000L)

        assertThat(robot.state.entries.map { it.id }).containsExactly(LogPlayerFixtures.THIRD_ENTRY_ID)

        robot.movePlayheadTo(positionMillis = 20_000L)

        assertThat(robot.state.entries.map { it.id }).containsExactly(LogPlayerFixtures.FOURTH_ENTRY_ID)

        robot.setTimeWindow(windowMillis = null)

        assertThat(robot.state.entries).hasSize(LogPlayerFixtures.mergedEntryIds.size)
    }
}
