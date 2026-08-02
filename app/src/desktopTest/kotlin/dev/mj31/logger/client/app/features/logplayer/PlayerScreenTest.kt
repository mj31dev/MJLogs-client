package dev.mj31.logger.client.app.features.logplayer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.player.VideoFrame
import kotlin.test.Test
import dev.mj31.logger.client.app.view.UiMessage
import dev.mj31.logger.client.app.features.logplayer.screen.PlayerScreen
import dev.mj31.logger.client.app.view.text.UiText

@OptIn(ExperimentalTestApi::class)
class PlayerScreenTest {

    @Test
    fun `shows the screencast pane, the log pane and the synchronization bar`() = runComposeUiTest {
        setContent {
            PlayerScreen(
                state = loadedState(),
                frame = mutableStateOf<VideoFrame?>(value = null),
                message = null,
                onIntent = {},
                onDismissMessage = {},
            )
        }

        onNodeWithText(text = "Screencast").assertIsDisplayed()
        onNodeWithText(text = "No screencast loaded").assertIsDisplayed()
        onNodeWithText(text = "Log session").assertIsDisplayed()
        onNodeWithText(text = "connected to server").assertIsDisplayed()
        onNodeWithText(text = "Timelines independent").assertIsDisplayed()
    }

    @Test
    fun `opening a video is requested from the empty pane`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent {
                PlayerScreen(
                    state = loadedState(),
                    frame = mutableStateOf<VideoFrame?>(value = null),
                    message = null,
                    onIntent = { intents += it },
                    onDismissMessage = {},
                )
            }

            // The header and the empty placeholder both offer it; either one must work.
            onAllNodesWithText(text = "Open video…").assertCountEquals(expectedSize = 2)
            onAllNodesWithText(text = "Open video…").onLast().performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.RequestVideoImport)
    }

    @Test
    fun `a transient message is shown until it is dismissed`() {
        var dismissed = false

        runComposeUiTest {
            setContent {
                PlayerScreen(
                    state = loadedState(),
                    frame = mutableStateOf<VideoFrame?>(value = null),
                    message = UiMessage(text = UiText.Raw(value = "Select a log record first")),
                    onIntent = {},
                    onDismissMessage = { dismissed = true },
                )
            }

            onNodeWithText(text = "Select a log record first").assertIsDisplayed()
            onNodeWithText(text = "Dismiss").performClick()
        }

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `a failure notice is styled apart from a confirmation`() = runComposeUiTest {
        setContent {
            PlayerScreen(
                state = loadedState(),
                frame = mutableStateOf<VideoFrame?>(value = null),
                message = UiMessage(text = UiText.Raw(value = "File not found"), isError = true),
                onIntent = {},
                onDismissMessage = {},
            )
        }

        onNodeWithText(text = "File not found").assertIsDisplayed()
        onNodeWithText(text = "Dismiss").assertIsDisplayed()
    }

    @Test
    fun `an unrecognized format opens the dialog over the workspace`() = runComposeUiTest {
        setContent {
            PlayerScreen(
                state = loadedState().copy(formatRequest = pendingFormatRequest()),
                frame = mutableStateOf<VideoFrame?>(value = null),
                message = null,
                onIntent = {},
                onDismissMessage = {},
            )
        }

        onNodeWithText(text = "Describe the format of analytics.txt").assertIsDisplayed()
        onNodeWithText(text = "Apply").assertIsDisplayed()
    }
}
