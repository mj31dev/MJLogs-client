package dev.mj31.logger.client.app.features.logplayer.duplicate

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateChoice
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateKind
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_already_open
import dev.mj31.logger.client.app.resources.message_merged
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.source.TextFileContent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * A file that is already in the session, in whole or in part.
 *
 * The fixture file `app.txt` holds three records at +0s, +20s and +40s.
 */
class LogPlayerStoreDuplicateTest {

    @Test
    fun `the same path is refused before anything is read`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        assertThat(robot.state.sources).hasSize(1)
        assertThat(robot.state.duplicateRequest).isNull()
        assertThat(robot.lastMessage).isEqualTo(
            UiText.Resource(resource = Res.string.message_already_open, arguments = listOf(LogPlayerFixtures.FIRST_NAME)),
        )
    }

    @Test
    fun `a copy under another path is put to the user, and skipping it adds nothing`() = runTest {
        val robot = copyImported()

        val request = robot.state.duplicateRequest
        assertThat(request?.kind).isEqualTo(DuplicateKind.COPY)
        assertThat(request?.existingName).isEqualTo(LogPlayerFixtures.FIRST_NAME)
        assertThat(request?.sharedRecordCount).isEqualTo(3)
        assertThat(request?.choices).containsExactly(DuplicateChoice.SKIP, DuplicateChoice.ADD_SEPARATELY).inOrder()

        robot.dispatch(intent = LogPlayerIntent.ResolveDuplicate(choice = DuplicateChoice.SKIP))

        assertThat(robot.state.duplicateRequest).isNull()
        assertThat(robot.state.sources).hasSize(1)
    }

    @Test
    fun `a copy can still be opened on purpose`() = runTest {
        val robot = copyImported()

        robot.dispatch(intent = LogPlayerIntent.ResolveDuplicate(choice = DuplicateChoice.ADD_SEPARATELY))

        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME, "app.txt")
        assertThat(robot.state.totalEntryCount).isEqualTo(6)
    }

    @Test
    fun `an overlapping file merges into the open one with each shared record once`() = runTest {
        val robot = overlapImported(spec = LogPlayerFixtures.FIRST_SPEC)
        val request = robot.state.duplicateRequest
        assertThat(request?.kind).isEqualTo(DuplicateKind.OVERLAP)
        assertThat(request?.sharedRecordCount).isEqualTo(1)

        robot.dispatch(intent = LogPlayerIntent.ResolveDuplicate(choice = DuplicateChoice.MERGE))

        val merged = robot.sessionRepository.sources.value.single()
        assertThat(merged.entries.map { it.message }).containsExactly(
            "Connected",
            "Retrying handshake",
            "Crash detected",
            "Recovered",
        ).inOrder()
        assertThat(merged.extraParts.single().path).isEqualTo(ROTATED_PATH)
        assertThat(merged.entries.last().sourceId).isEqualTo(merged.id)
        assertThat(robot.state.sources.single().fileCount).isEqualTo(2)
        assertThat(robot.lastMessage).isEqualTo(
            UiText.Resource(
                resource = Res.string.message_merged,
                arguments = listOf("app.1.txt", LogPlayerFixtures.FIRST_NAME, 1),
            ),
        )
    }

    @Test
    fun `an overlapping file read under another format is simply opened`() = runTest {
        val robot = overlapImported(spec = LogPlayerFixtures.SECOND_SPEC)

        assertThat(robot.state.duplicateRequest).isNull()
        assertThat(robot.state.sources).hasSize(2)
    }

    private fun TestScope.copyImported(): LogPlayerRobot {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.files.register(content = LogPlayerFixtures.firstFile.copy(path = COPY_PATH))
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH, COPY_PATH))
        return robot
    }

    private fun TestScope.overlapImported(
        spec: LogFormatSpec,
    ): LogPlayerRobot {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.files.register(
            content = TextFileContent(
                path = ROTATED_PATH,
                name = "app.1.txt",
                lines = listOf(
                    LogPlayerFixtures.firstFile.lines.last(),
                    "${LogPlayerFixtures.at(offsetMillis = 50_000L).toEpochMilliseconds()}|INFO|Network|Recovered",
                ),
            ),
        )
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.detector.enqueueDetected(spec = spec)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH, ROTATED_PATH))
        return robot
    }

    private companion object {
        const val COPY_PATH = "/backup/app.txt"
        const val ROTATED_PATH = "/logs/app.1.txt"
    }
}
