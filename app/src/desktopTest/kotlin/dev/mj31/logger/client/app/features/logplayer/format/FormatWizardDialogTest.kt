package dev.mj31.logger.client.app.features.logplayer.format

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlin.test.Test
import dev.mj31.logger.client.data.format.preview.RegexLogFormatPreviewer
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatError

@OptIn(ExperimentalTestApi::class)
class FormatWizardDialogTest {

    @Test
    fun `opens on the inferred layout and reports how it reads the sample`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = {}) }

        onNodeWithText(text = "Describe the format of analytics.txt").assertIsDisplayed()
        onNodeWithText(text = TIMESTAMP_PATTERN).assertIsDisplayed()
        onNodeWithText(text = STRUCTURE_TEMPLATE).assertIsDisplayed()
        onNodeWithText(text = "2 of 2 sample lines become records").assertIsDisplayed()
        onNodeWithText(text = "Apply").assertIsEnabled()
    }

    @Test
    fun `editing a field reports the whole draft`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = TIMESTAMP_PATTERN).performTextReplacement(text = "HH:mm:ss")
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.UpdateFormatDraft(
                timestampPattern = "HH:mm:ss",
                structureTemplate = STRUCTURE_TEMPLATE,
            ),
        )
    }

    @Test
    fun `a draft that reads nothing cannot be applied`() = runComposeUiTest {
        val request = requestFor(
            draft = ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}"),
        )

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        onNodeWithText(text = "0 of 2 sample lines become records").assertIsDisplayed()
        onNodeWithText(text = "Apply").assertIsNotEnabled()
    }

    @Test
    fun `a broken timestamp pattern is reported under its own field`() = runComposeUiTest {
        val request = requestFor(
            draft = ManualFormatInput(timestampPattern = "???", structureTemplate = "{timestamp} {message}"),
        )

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        assertThat(request.timestampPatternError).isNotNull()
        assertThat(request.structureTemplateError).isNull()
        onNodeWithText(text = request.timestampPatternError.orEmpty()).assertIsDisplayed()
        onNodeWithText(text = "No preview while the format is invalid").assertIsDisplayed()
        onNodeWithText(text = "Apply").assertIsNotEnabled()
    }

    @Test
    fun `a broken structure template is reported under its own field`() = runComposeUiTest {
        val request = requestFor(
            draft = ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {thread}"),
        )

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        assertThat(request.structureTemplateError).contains("{thread}")
        assertThat(request.timestampPatternError).isNull()
        onNodeWithText(text = request.structureTemplateError.orEmpty()).assertIsDisplayed()
    }

    @Test
    fun `a failure that belongs to no field falls back to a notice`() = runComposeUiTest {
        val draft = ManualFormatInput(timestampPattern = TIMESTAMP_PATTERN, structureTemplate = STRUCTURE_TEMPLATE)
        val request = requestFor(draft = draft).copy(
            error = FormatError(message = "No line matched the provided format", field = FormatErrorField.NONE),
        )

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        assertThat(request.timestampPatternError).isNull()
        assertThat(request.structureTemplateError).isNull()
        onNodeWithText(text = "No line matched the provided format").assertIsDisplayed()
    }

    @Test
    fun `applying submits the drafted format`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = "Apply").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SubmitManualFormat)
    }

    @Test
    fun `skipping the file dismisses the request`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = "Skip file").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.DismissFormatRequest)
    }

    @Test
    fun `a preset replaces both fields at once`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = "Time only").performClick()
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.UpdateFormatDraft(
                timestampPattern = "HH:mm:ss",
                structureTemplate = "{timestamp} {message}",
            ),
        )
    }

    @Test
    fun `the field keeps what was typed before the state travels back`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = {}) }

        onNodeWithText(text = TIMESTAMP_PATTERN).performTextReplacement(text = "HH:mm:ss")

        // The store answers a frame later; an externally driven field would have snapped back here.
        onNodeWithText(text = "HH:mm:ss").assertIsDisplayed()
    }

    @Test
    fun `undo walks backwards through the edits instead of bouncing forward`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = {}) }

        onNodeWithText(text = TIMESTAMP_PATTERN).performTextReplacement(text = "HH:mm:ss")
        onNode(matcher = isFocused()).performTextInput(text = ".SSS")
        onNodeWithText(text = "HH:mm:ss.SSS").assertIsDisplayed()

        onNode(matcher = isFocused()).performUndo()
        onNodeWithText(text = "HH:mm:ss.SSS").assertDoesNotExist()

        // The reported defect: the second undo used to jump forward to the newest text again.
        onNode(matcher = isFocused()).performUndo()
        onNodeWithText(text = "HH:mm:ss.SSS").assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.performUndo(): SemanticsNodeInteraction = performKeyInput {
        withKeyDown(key = UNDO_MODIFIER) { pressKey(key = Key.Z) }
    }

    @Test
    fun `a confirmation offers to keep the file as it was read`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val request = requestFor(
            draft = ManualFormatInput(timestampPattern = TIMESTAMP_PATTERN, structureTemplate = STRUCTURE_TEMPLATE),
        ).copy(detectedSource = detectedSource())

        runComposeUiTest {
            setContent { FormatWizardDialog(request = request, onIntent = { intents += it }) }

            onNodeWithText(text = "Check how analytics.txt was read").assertIsDisplayed()
            onNodeWithText(text = "Use my format").assertIsDisplayed()
            onNodeWithText(text = "Import as detected").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.AcceptDetectedFormat)
    }

    @Test
    fun `an unrecognized file has nothing to keep`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = suggestedRequest(), onIntent = {}) }

        onNodeWithText(text = "Import as detected").assertDoesNotExist()
        onNodeWithText(text = "Apply").assertIsDisplayed()
    }

    private fun detectedSource(): LogSource = LogSource(
        id = "src-1",
        name = "analytics.txt",
        path = "/logs/analytics.txt",
        format = LogFormatSpec(
            name = "date time millis - message only",
            linePattern = "(?<ts>.*)",
            timestampPattern = TIMESTAMP_PATTERN,
        ),
        entries = emptyList(),
    )

    private fun suggestedRequest(): FormatRequestUiState = requestFor(
        draft = ManualFormatInput(timestampPattern = TIMESTAMP_PATTERN, structureTemplate = STRUCTURE_TEMPLATE),
        suggested = true,
    )

    private fun requestFor(draft: ManualFormatInput, suggested: Boolean = false): FormatRequestUiState =
        FormatRequestUiState(
            path = "/logs/analytics.txt",
            fileName = "analytics.txt",
            sampleLines = SAMPLE_LINES,
            reason = "No built-in log format matched any line of the sample.",
            timestampPattern = draft.timestampPattern,
            structureTemplate = draft.structureTemplate,
            preview = RegexLogFormatPreviewer().preview(input = draft, sampleLines = SAMPLE_LINES),
            suggestion = draft.takeIf { suggested },
        )

    private companion object {

        /** Compose maps undo to the platform modifier, so the test has to use the same one. */
        val UNDO_MODIFIER: Key = if (System.getProperty("os.name").orEmpty().startsWith(prefix = "Mac")) {
            Key.MetaLeft
        } else {
            Key.CtrlLeft
        }

        const val TIMESTAMP_PATTERN = "dd.MM.yyyy_HH.mm.ss"
        const val STRUCTURE_TEMPLATE = "<{any}>~{timestamp}~{tag}~{message}"

        val SAMPLE_LINES = listOf(
            "<0000>~01.08.2026_10.23.45~ANALYTICS~event dispatched (0)",
            "<0001>~01.08.2026_10.23.46~ANALYTICS~event dispatched (1)",
        )
    }
}
