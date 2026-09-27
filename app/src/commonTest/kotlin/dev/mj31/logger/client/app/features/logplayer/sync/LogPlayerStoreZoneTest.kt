package dev.mj31.logger.client.app.features.logplayer.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Choosing the zone a file, or the clock on the screen, is read in.
 *
 * The fixture session starts at `2024-05-01T10:00:00Z` and its files carry no zone of their own, so
 * they are read in UTC until someone says otherwise.
 */
class LogPlayerStoreZoneTest {

    @Test
    fun `the zone choice for a file names the file and what it is read in now`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        val first = robot.state.sources.first()

        robot.dispatch(intent = LogPlayerIntent.RequestSourceZone(sourceId = first.id))

        val request = robot.state.zoneRequest
        assertThat(request?.target).isEqualTo(ZoneTarget.Source(sourceId = first.id))
        assertThat(request?.subject).isEqualTo(first.name)
        assertThat(request?.current).isEqualTo(SourceZone.UTC)
        assertThat(request?.availableZoneIds).contains("Europe/Berlin")
    }

    @Test
    fun `choosing a zone for a file reads it again in that zone and shows UTC beside it`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        val first = robot.state.sources.first()
        assertThat(robot.state.showUtcColumn).isFalse()

        robot.dispatch(intent = LogPlayerIntent.RequestSourceZone(sourceId = first.id))
        robot.dispatch(intent = LogPlayerIntent.ChooseZone(zoneId = "UTC+02:00"))

        val zone = SourceZone(id = "UTC+02:00", origin = ZoneOrigin.CHOSEN)
        assertThat(robot.state.zoneRequest).isNull()
        assertThat(robot.state.sources.first { it.id == first.id }.zone).isEqualTo(zone)
        assertThat(robot.state.sourceTimeZones[first.id]).isEqualTo(zone.timeZone)
        assertThat(robot.state.showUtcColumn).isTrue()
    }

    @Test
    fun `a typed frame time is read in the zone chosen for the screen`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        robot.loadVideo(positionMillis = 10_000L)

        robot.dispatch(intent = LogPlayerIntent.RequestFrameTimeZone)
        robot.dispatch(intent = LogPlayerIntent.ChooseZone(zoneId = "UTC+01:00"))
        robot.typeFrameTime(text = "11:00:20")
        robot.synchronizeAtFrameTime()
        robot.movePlayheadTo(positionMillis = 30_000L)

        assertThat(robot.state.sync.zone).isEqualTo(SourceZone(id = "UTC+01:00", origin = ZoneOrigin.CHOSEN))
        assertThat(robot.state.sync.logTimeAtPlayhead).isEqualTo(LogPlayerFixtures.at(offsetMillis = 40_000L))
    }

    @Test
    fun `handing the screen's zone back makes it follow the logs again`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.dispatch(intent = LogPlayerIntent.RequestFrameTimeZone)
        robot.dispatch(intent = LogPlayerIntent.ChooseZone(zoneId = "UTC+01:00"))
        robot.dispatch(intent = LogPlayerIntent.RequestFrameTimeZone)
        robot.dispatch(intent = LogPlayerIntent.ChooseZone(zoneId = null))

        assertThat(robot.state.sync.zone).isEqualTo(SourceZone.UTC)
    }

    @Test
    fun `the screen follows the file of the selected record`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()
        val second = robot.state.sources.last()
        robot.dispatch(intent = LogPlayerIntent.RequestSourceZone(sourceId = second.id))
        robot.dispatch(intent = LogPlayerIntent.ChooseZone(zoneId = "Asia/Tokyo"))

        robot.selectEntry(entryId = robot.state.entries.first { it.sourceId == second.id }.id)

        assertThat(robot.state.sync.zone.id).isEqualTo("Asia/Tokyo")
    }

    @Test
    fun `dismissing the choice changes nothing`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.dispatch(intent = LogPlayerIntent.RequestFrameTimeZone)
        robot.dispatch(intent = LogPlayerIntent.DismissZoneRequest)

        assertThat(robot.state.zoneRequest).isNull()
        assertThat(robot.state.sync.zone).isEqualTo(SourceZone.UTC)
    }
}
