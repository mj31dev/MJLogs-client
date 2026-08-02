package dev.mj31.logger.client.app.features.logplayer

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.LogComponent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class LogPlayerStoreManualFormatTest {

    @Test
    fun `a manual format imports the file and closes the request`() = runTest {
        val robot = robotWithPendingRequest(path = LogPlayerFixtures.FIRST_PATH)

        robot.submitManualFormat(timestampPattern = "epochMillis", structureTemplate = "{timestamp}|{level}|{tag}|{message}")

        assertThat(robot.state.formatRequest).isNull()
        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME)
        assertThat(robot.state.sources.single().formatName).isEqualTo(LogPlayerFixtures.MANUAL_SPEC.name)
        assertThat(robot.compiler.inputs.single().timestampPattern).isEqualTo("epochMillis")
    }

    @Test
    fun `a rejected pattern is reported on the input that caused it`() = runTest {
        val robot = robotWithPendingRequest(path = LogPlayerFixtures.FIRST_PATH)
        robot.compiler.result = FormatCompilationResult.Failure(
            message = "Unknown token 'qq'",
            field = FormatErrorField.TIMESTAMP_PATTERN,
        )

        robot.submitManualFormat()

        val request = requireNotNull(robot.state.formatRequest)
        assertThat(request.timestampPatternError).isEqualTo("Unknown token 'qq'")
        assertThat(request.structureTemplateError).isNull()
        assertThat(request.generalError).isNull()
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `a format matching no line falls back to a general error`() = runTest {
        val robot = robotWithPendingRequest(path = LogPlayerFixtures.UNPARSABLE_PATH)

        robot.submitManualFormat()

        val request = requireNotNull(robot.state.formatRequest)
        // Neither input is malformed, so blaming one of them would be misleading.
        assertThat(request.generalError).isEqualTo("No line matched the provided format")
        assertThat(request.timestampPatternError).isNull()
        assertThat(request.structureTemplateError).isNull()
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `editing the draft refreshes the preview of the sample`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        val sample = LogPlayerFixtures.line(offsetMillis = 0L)
        robot.detector.enqueueUndetermined(sampleLines = listOf(sample), reason = "unknown layout")
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.updateFormatDraft(
            timestampPattern = "epochMillis",
            structureTemplate = "{timestamp}|{level}|{tag}|{message}",
        )

        val request = requireNotNull(robot.state.formatRequest)
        assertThat(request.timestampPattern).isEqualTo("epochMillis")
        val ready = assertIs<FormatPreview.Ready>(request.preview)
        assertThat(ready.matchedLines).isEqualTo(1)
        assertThat(ready.lines.single().spans.map { it.component })
            .containsExactly(LogComponent.TIMESTAMP, LogComponent.LEVEL, LogComponent.TAG, LogComponent.MESSAGE)
            .inOrder()
        assertThat(request.canApply).isTrue()
    }

    @Test
    fun `a draft that compiles but matches nothing cannot be applied`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueUndetermined(sampleLines = listOf("an entirely unfamiliar line"), reason = "unknown")
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.updateFormatDraft(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}")

        val request = requireNotNull(robot.state.formatRequest)
        assertThat(assertIs<FormatPreview.Ready>(request.preview).matchedLines).isEqualTo(0)
        assertThat(request.canApply).isFalse()
    }

    @Test
    fun `an uncompilable draft is reported live`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueUndetermined(sampleLines = listOf("an entirely unfamiliar line"), reason = "unknown")
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.updateFormatDraft(timestampPattern = "???", structureTemplate = "{timestamp} {message}")

        val request = requireNotNull(robot.state.formatRequest)
        assertThat(request.preview).isInstanceOf(FormatPreview.Invalid::class.java)
        assertThat(request.timestampPatternError).isNotNull()
        assertThat(request.structureTemplateError).isNull()
        assertThat(request.canApply).isFalse()
    }

    @Test
    fun `skipping the file drops the request`() = runTest {
        val robot = robotWithPendingRequest(path = LogPlayerFixtures.FIRST_PATH)

        robot.dismissFormatRequest()

        assertThat(robot.state.formatRequest).isNull()
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `requests are answered one file at a time`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueUndetermined(sampleLines = listOf("line one"), reason = "unknown")
        robot.detector.enqueueUndetermined(sampleLines = listOf("line two"), reason = "unknown")

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH, LogPlayerFixtures.SECOND_PATH))

        assertThat(robot.state.formatRequest?.path).isEqualTo(LogPlayerFixtures.FIRST_PATH)

        robot.dismissFormatRequest()

        assertThat(robot.state.formatRequest?.path).isEqualTo(LogPlayerFixtures.SECOND_PATH)
    }

    @Test
    fun `submitting without a pending request is a no-op`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.submitManualFormat()

        assertThat(robot.compiler.inputs).isEmpty()
    }

    @Test
    fun `an incomplete detection opens the dialog with the parsed file behind it`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetectedButIncomplete(spec = LogPlayerFixtures.FIRST_SPEC)

        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        val request = requireNotNull(robot.state.formatRequest)
        assertThat(request.isConfirmation).isTrue()
        assertThat(request.fileName).isEqualTo(LogPlayerFixtures.FIRST_NAME)
        assertThat(request.reason).contains("level")
        // Nothing is imported until the user answers.
        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `confirming keeps the file exactly as it was read`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetectedButIncomplete(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.dispatch(intent = LogPlayerIntent.AcceptDetectedFormat)

        assertThat(robot.state.formatRequest).isNull()
        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.FIRST_NAME)
        assertThat(robot.state.totalEntryCount).isEqualTo(3)
        assertThat(robot.lastMessageArguments).contains(LogPlayerFixtures.FIRST_NAME)
    }

    @Test
    fun `declining the confirmation drops the file`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetectedButIncomplete(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))

        robot.dismissFormatRequest()

        assertThat(robot.state.formatRequest).isNull()
        assertThat(robot.state.sources).isEmpty()
    }

    private fun TestScope.robotWithPendingRequest(path: String): LogPlayerRobot {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueUndetermined(sampleLines = listOf("unreadable line"), reason = "unknown layout")
        robot.importLogFiles(paths = listOf(path))
        return robot
    }
}
