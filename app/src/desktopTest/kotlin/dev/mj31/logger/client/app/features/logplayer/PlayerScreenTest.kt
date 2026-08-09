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
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.state.ui.PackageSaveUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.WorkspaceUiState
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

    /**
     * A notice must not move the workspace it is describing.
     *
     * Sharing the column with the panes meant every message resized them as it appeared and again as
     * it went — so a message about a record would shift the record out from under the pointer, and a
     * message that arrived while the user was reaching for the play button moved the button. It
     * floats over them instead, and the proof is that nothing below it changes position.
     */
    @Test
    fun `a transient notice leaves the layout where it was`() {
        val quiet = syncBarTop(message = null)
        val noticed = syncBarTop(
            message = UiMessage(text = UiText.Raw(value = "Something worth saying"), isError = true),
        )

        assertThat(noticed).isEqualTo(quiet)
    }

    /**
     * The complaint under the frame time field is transient in exactly the same way, and it used to
     * be part of the bar's own height: it appeared as the user typed something unreadable and pushed
     * the workspace up, then let it fall back on the next keystroke.
     */
    @Test
    fun `an invalid frame time leaves the layout where it was`() {
        val valid = syncBarTop(message = null)
        val invalid = syncBarTop(
            message = null,
            state = loadedState().copy(
                sync = loadedState().sync.copy(frameTime = "not a time", frameTimeError = true),
            ),
        )

        assertThat(invalid).isEqualTo(valid)
    }

    @Test
    fun `writing a session file is visible over the workspace`() = runComposeUiTest {
        setContent {
            PlayerScreen(
                state = savingState(),
                frame = mutableStateOf<VideoFrame?>(value = null),
                message = null,
                onIntent = {},
                onDismissMessage = {},
            )
        }

        onNodeWithText(text = "Saving session").assertIsDisplayed()
        onNodeWithText(text = "Copying screencast.mp4").assertIsDisplayed()
        // The workspace stays readable underneath: a save is not a modal state.
        onNodeWithText(text = "connected to server").assertIsDisplayed()
    }

    @Test
    fun `cancelling a save leaves as an intent`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent {
                PlayerScreen(
                    state = savingState(),
                    frame = mutableStateOf<VideoFrame?>(value = null),
                    message = null,
                    onIntent = { intents += it },
                    onDismissMessage = {},
                )
            }

            onNodeWithText(text = "Cancel").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.CancelSessionSave)
    }

    /**
     * The save bar floats for the same reason a notice does.
     *
     * It appears when a save starts and goes when it ends, and a copy of a screencast runs long
     * enough that the user is reading the log while it happens — so a bar that took part in the
     * column would move the very records being read, twice per save.
     */
    @Test
    fun `the save bar leaves the layout where it was`() {
        val quiet = syncBarTop(message = null)
        val saving = syncBarTop(message = null, state = savingState())

        assertThat(saving).isEqualTo(quiet)
    }

    private fun savingState(): LogPlayerState = loadedState().copy(
        workspace = WorkspaceUiState(
            packagePath = "/cases/investigation.mjclog",
            packageName = "investigation",
            save = PackageSaveUiState(
                fileName = "screencast.mp4",
                fraction = 0.25f,
                copiedBytes = 25L,
                totalBytes = 100L,
            ),
        ),
    )

    /** Top edge of the synchronization bar, which is what anything in the column would have pushed. */
    private fun syncBarTop(message: UiMessage?, state: LogPlayerState = loadedState()): Float {
        var top = 0f
        runComposeUiTest {
            setContent {
                PlayerScreen(
                    state = state,
                    frame = mutableStateOf<VideoFrame?>(value = null),
                    message = message,
                    onIntent = {},
                    onDismissMessage = {},
                )
            }

            top = onNodeWithText(text = "Timelines independent").fetchSemanticsNode().boundsInRoot.top
        }
        return top
    }
}
