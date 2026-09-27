package dev.mj31.logger.client.app.features.logplayer.format

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatKind
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.data.format.preview.ManualFormatPreviewer
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import kotlin.test.Test

/**
 * The dialog against the two shapes that are not a line layout.
 *
 * Detection guesses the shape and guesses it wrong exactly when this dialog opens, so saying which
 * one it really is has to be possible here rather than by cancelling and importing again.
 */
@OptIn(ExperimentalTestApi::class)
class FormatWizardShapesTest {

    @Test
    fun `the three shapes a log can have are all on offer`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = jsonRequest(), onIntent = {}) }

        onNodeWithText(text = "Plain lines").assertIsDisplayed()
        onNodeWithText(text = "JSON per line").assertIsDisplayed()
        onNodeWithText(text = "Columns").assertIsDisplayed()
    }

    @Test
    fun `saying the file is another shape asks for the switch`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = jsonRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = "Columns").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SelectFormatKind(kind = FormatKind.DELIMITED))
    }

    @Test
    fun `a JSON log is described by the keys its components live under`() = runComposeUiTest {
        setContent { FormatWizardDialog(request = jsonRequest(), onIntent = {}) }

        onNodeWithText(text = "Timestamp key").assertIsDisplayed()
        onNodeWithText(text = "Level key").assertIsDisplayed()
        onNodeWithText(text = "Tag key").assertIsDisplayed()
        onNodeWithText(text = "Message key").assertIsDisplayed()
        onNodeWithText(text = "2 of 2 sample lines become records").assertIsDisplayed()
    }

    @Test
    fun `typing a JSON key reports a JSON draft`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { FormatWizardDialog(request = jsonRequest(), onIntent = { intents += it }) }

            onNodeWithText(text = "log.level").performTextReplacement(text = "severity")
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.UpdateFormatDraft(
                draft = ManualFormatInput.Json(
                    timestampPattern = ISO_PATTERN,
                    timestampKey = "ts",
                    levelKey = "severity",
                    tagKey = "",
                    messageKey = "msg",
                ),
            ),
        )
    }

    @Test
    fun `a separator that is not one character is reported under its own field`() = runComposeUiTest {
        val request = requestFor(draft = delimitedDraft().copy(delimiter = ",,"), sampleLines = CSV_LINES)

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        assertThat(request.errorFor(field = FormatErrorField.DELIMITER)).isNotNull()
        assertThat(request.timestampPatternError).isNull()
        onNodeWithText(text = request.errorFor(field = FormatErrorField.DELIMITER).orEmpty()).assertIsDisplayed()
        onNodeWithText(text = "Apply").assertIsNotEnabled()
    }

    @Test
    fun `a column no header can name is reported under that column`() = runComposeUiTest {
        val request = requestFor(
            draft = delimitedDraft().copy(hasHeader = false, timestampField = "time"),
            sampleLines = CSV_LINES,
        )

        setContent { FormatWizardDialog(request = request, onIntent = {}) }

        assertThat(request.errorFor(field = FormatErrorField.TIMESTAMP_FIELD)).isNotNull()
        onNodeWithText(text = request.errorFor(field = FormatErrorField.TIMESTAMP_FIELD).orEmpty()).assertIsDisplayed()
    }

    private fun jsonRequest(): FormatRequestUiState = requestFor(
        draft = ManualFormatInput.Json(
            timestampPattern = ISO_PATTERN,
            timestampKey = "ts",
            levelKey = "log.level",
            messageKey = "msg",
        ),
        sampleLines = JSON_LINES,
    )

    private fun delimitedDraft(): ManualFormatInput.Delimited = ManualFormatInput.Delimited(
        timestampPattern = ISO_PATTERN,
        delimiter = ",",
        hasHeader = true,
        timestampField = "time",
        levelField = "level",
        messageField = "message",
    )

    /** The previewer that answers for all three shapes, which is the one the store hands the dialog. */
    private fun requestFor(draft: ManualFormatInput, sampleLines: List<String>): FormatRequestUiState =
        FormatRequestUiState(
            path = "/logs/analytics.txt",
            fileName = "analytics.txt",
            sampleLines = sampleLines,
            reason = "No built-in log format matched any line of the sample.",
            draft = draft,
            preview = ManualFormatPreviewer().preview(input = draft, sampleLines = sampleLines),
        )

    private companion object {

        const val ISO_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"

        val JSON_LINES = listOf(
            """{"ts":"2026-08-01 10:23:45.100","log":{"level":"INFO"},"msg":"event dispatched"}""",
            """{"ts":"2026-08-01 10:23:46.200","log":{"level":"WARN"},"msg":"retrying"}""",
        )

        val CSV_LINES = listOf(
            "time,level,message",
            "2026-08-01 10:23:45.100,INFO,event dispatched",
            "2026-08-01 10:23:46.200,WARN,retrying",
        )
    }
}
