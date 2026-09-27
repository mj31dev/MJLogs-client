package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.SourcePreambleUiState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SourcePreambleDialogTest {

    private val preamble = SourcePreambleUiState(
        sourceId = "src-1",
        fileName = "device.log",
        lines = listOf("=== MyApp 1.4.2 ===", "Device: Pixel 8"),
    )

    @Test
    fun `names the file and shows the header verbatim`() = runComposeUiTest {
        setContent { SourcePreambleDialog(preamble = preamble, onIntent = {}) }

        onNodeWithText(text = "device.log").assertIsDisplayed()
        onNodeWithText(text = "=== MyApp 1.4.2 ===", substring = true).assertIsDisplayed()
        onNodeWithText(text = "Device: Pixel 8", substring = true).assertIsDisplayed()
    }

    @Test
    fun `closing asks the store to dismiss it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { SourcePreambleDialog(preamble = preamble, onIntent = { intents += it }) }

            onNodeWithTag(testTag = PREAMBLE_DISMISS_TAG).performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.DismissSourcePreamble)
    }
}
