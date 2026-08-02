package dev.mj31.logger.client.app.features.logplayer

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.app.view.text.UiText

class LogPlayerStoreImportTest {

    @Test
    fun `importing several files merges their records chronologically`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importBothLogFiles()

        assertThat(robot.state.entries.map { it.id }).containsExactlyElementsIn(LogPlayerFixtures.mergedEntryIds).inOrder()
        assertThat(robot.state.totalEntryCount).isEqualTo(LogPlayerFixtures.mergedEntryIds.size)
        assertThat(robot.state.hasLogs).isTrue()
    }

    @Test
    fun `every imported file is described as a source`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importBothLogFiles()

        val sources = robot.state.sources
        assertThat(sources.map { it.name })
            .containsExactly(LogPlayerFixtures.FIRST_NAME, LogPlayerFixtures.SECOND_NAME)
            .inOrder()
        assertThat(sources.map { it.entryCount }).containsExactly(3, 2).inOrder()
        assertThat(sources.map { it.formatName })
            .containsExactly(LogPlayerFixtures.FIRST_SPEC.name, LogPlayerFixtures.SECOND_SPEC.name)
            .inOrder()
        assertThat(sources.all { it.isSelected }).isTrue()
    }

    @Test
    fun `an unrecognized format asks the user instead of importing the file`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        val samples = listOf("an entirely unfamiliar line")
        robot.detector.enqueueUndetermined(sampleLines = samples, reason = "No candidate matched")

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.UNPARSABLE_PATH))

        val request = robot.state.formatRequest
        assertThat(request).isNotNull()
        assertThat(request?.path).isEqualTo(LogPlayerFixtures.UNPARSABLE_PATH)
        assertThat(request?.fileName).isEqualTo(LogPlayerFixtures.UNPARSABLE_NAME)
        assertThat(request?.sampleLines).isEqualTo(samples)
        assertThat(request?.reason).isEqualTo("No candidate matched")
        assertThat(robot.state.totalEntryCount).isEqualTo(0)
    }

    @Test
    fun `the inferred format reaches the dialog so it can be pre-filled`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        val suggestion = ManualFormatInput(
            timestampPattern = "dd.MM.yyyy_HH.mm.ss",
            structureTemplate = "<{any}>~{timestamp}~{tag}~{message}",
        )
        robot.detector.enqueueUndetermined(
            sampleLines = listOf("<0000>~01.08.2026_10.23.45~ANALYTICS~event dispatched (0)"),
            reason = "No candidate matched",
            suggestion = suggestion,
        )

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.UNPARSABLE_PATH))

        assertThat(robot.state.formatRequest?.suggestion).isEqualTo(suggestion)
    }

    @Test
    fun `a detected format that matches nothing also asks the user`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.UNPARSABLE_PATH))

        assertThat(robot.state.formatRequest?.reason).isEqualTo("Detected format produced no records")
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `an unreadable file reports a message and leaves the session untouched`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.files.registerFailure(
            path = LogPlayerFixtures.MISSING_PATH,
            error = IllegalArgumentException("File not found"),
        )

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.MISSING_PATH))

        assertThat(robot.lastMessage).isEqualTo(UiText.Raw(value = "File not found"))
        assertThat(robot.state.formatRequest).isNull()
        assertThat(robot.state.totalEntryCount).isEqualTo(0)
    }

    @Test
    fun `a failing file does not stop the remaining imports`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.files.registerFailure(path = LogPlayerFixtures.MISSING_PATH, error = IllegalStateException("boom"))
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.MISSING_PATH, LogPlayerFixtures.FIRST_PATH))

        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME)
        assertThat(robot.state.isImporting).isFalse()
    }

    @Test
    fun `importing nothing is a no-op`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importLogFiles(paths = emptyList())

        assertThat(robot.state.isImporting).isFalse()
        assertThat(robot.files.requestedPaths).isEmpty()
    }

    @Test
    fun `loading a screencast opens it in the player`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.loadVideo()

        assertThat(robot.player.openedMedia.map { it.path })
            .containsExactly(LogPlayerRobot.DEFAULT_VIDEO_PATH)
        assertThat(robot.state.video.name).isEqualTo(LogPlayerRobot.DEFAULT_VIDEO_NAME)
        assertThat(robot.state.video.hasVideo).isTrue()
    }

    @Test
    fun `a successful import reports what was recognized`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        assertThat(robot.lastMessageArguments).contains(LogPlayerFixtures.FIRST_NAME)
        assertThat(robot.lastMessageArguments).contains(LogPlayerFixtures.FIRST_SPEC.name)
    }

    @Test
    fun `asking for files is answered with a picker effect and never with state`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.dispatch(intent = LogPlayerIntent.RequestLogImport)
        robot.dispatch(intent = LogPlayerIntent.RequestVideoImport)

        assertThat(robot.effects)
            .containsExactly(LogPlayerEffect.PickLogFiles, LogPlayerEffect.PickVideoFile)
            .inOrder()
        assertThat(robot.state.sources).isEmpty()
    }
}
