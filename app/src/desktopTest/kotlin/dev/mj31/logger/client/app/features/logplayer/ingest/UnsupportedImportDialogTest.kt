package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.UnsupportedImportUiState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class UnsupportedImportDialogTest {

    @Test
    fun `names the file it is asking about and explains the refusal`() = runComposeUiTest {
        setContent { UnsupportedImportDialog(request = refusedFile(), onIntent = {}) }

        onNodeWithText(text = "dmesg.trace").assertIsDisplayed()
        onNodeWithText(text = REASON).assertIsDisplayed()
        onNodeWithText(text = "Read it as a log anyway?").assertIsDisplayed()
    }

    @Test
    fun `insisting imports the file regardless of its name`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { UnsupportedImportDialog(request = refusedFile(), onIntent = { intents += it }) }

            onNodeWithText(text = "Open anyway").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ConfirmUnsupportedImport)
    }

    @Test
    fun `skipping lets the file go`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { UnsupportedImportDialog(request = refusedFile(), onIntent = { intents += it }) }

            onNodeWithText(text = "Skip file").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.DismissUnsupportedImport)
    }

    private fun refusedFile(): UnsupportedImportUiState = UnsupportedImportUiState(
        path = "/captures/2026-08-12/dmesg.trace",
        fileName = "dmesg.trace",
        reason = REASON,
    )

    private companion object {
        const val REASON = "dmesg.trace is not a supported log file; expected one of: .log, .txt, .ndjson."
    }
}
