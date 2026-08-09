package dev.mj31.logger.client.app.features.logplayer.session

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.state.ui.PackageSaveUiState
import kotlin.test.Test

/**
 * The bar shown while a session file is being written.
 *
 * A full package copies a screencast, so this is the only part of the application where the user
 * waits on it — which makes what it says, and the way out of it, worth asserting on.
 */
@OptIn(ExperimentalTestApi::class)
class SessionSaveBarTest {

    @Test
    fun `names the file being copied while the write runs`() = runComposeUiTest {
        setContent {
            SessionSaveBar(save = halfway(), onCancel = {})
        }

        onNodeWithText(text = "Saving session").assertIsDisplayed()
        onNodeWithText(text = "Copying screencast.mp4").assertIsDisplayed()
        onNodeWithText(text = "Cancel").assertIsDisplayed()
    }

    @Test
    fun `cancelling is one click away`() {
        var cancelled = false

        runComposeUiTest {
            setContent {
                SessionSaveBar(save = halfway(), onCancel = { cancelled = true })
            }

            onNodeWithText(text = "Cancel").performClick()
        }

        assertThat(cancelled).isTrue()
    }

    /**
     * A light package bundles nothing, so there are no bytes to count.
     *
     * The bar still has to appear and still has to offer the way out: what is unknown is how far
     * along it is, not whether it is running.
     */
    @Test
    fun `a write with nothing to copy still shows the bar and the way out`() = runComposeUiTest {
        setContent {
            SessionSaveBar(
                save = PackageSaveUiState(
                    fileName = "case.mjclog",
                    fraction = 0f,
                    copiedBytes = 0L,
                    totalBytes = 0L,
                ),
                onCancel = {},
            )
        }

        onNodeWithText(text = "Saving session").assertIsDisplayed()
        onNodeWithText(text = "Copying case.mjclog").assertIsDisplayed()
        onNodeWithText(text = "Cancel").assertIsDisplayed()
    }

    private fun halfway(): PackageSaveUiState = PackageSaveUiState(
        fileName = "screencast.mp4",
        fraction = 0.5f,
        copiedBytes = 50L,
        totalBytes = 100L,
    )
}
