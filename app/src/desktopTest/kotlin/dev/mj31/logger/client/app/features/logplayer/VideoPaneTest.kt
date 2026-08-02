package dev.mj31.logger.client.app.features.logplayer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import kotlin.test.Test
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.screen.VideoPane

@OptIn(ExperimentalTestApi::class)
class VideoPaneTest {

    @Test
    fun `an empty pane invites the user to open a screencast`() = runComposeUiTest {
        setContent { pane(video = VideoUiState(), onIntent = {}) }

        onNodeWithText(text = "No file loaded").assertIsDisplayed()
        onNodeWithText(text = "No screencast loaded").assertIsDisplayed()
        onNodeWithText(text = "Play").assertIsNotEnabled()
    }

    @Test
    fun `a loaded screencast without a frame yet reports that it is decoding`() = runComposeUiTest {
        setContent { pane(video = loaded(), onIntent = {}) }

        onNodeWithText(text = "clip.mp4").assertIsDisplayed()
        onNodeWithText(text = "Decoding…").assertIsDisplayed()
        onNodeWithText(text = "Replace…").assertIsDisplayed()
    }

    @Test
    fun `a paused screencast offers play and a playing one offers pause`() = runComposeUiTest {
        setContent { pane(video = loaded(status = PlaybackStatus.PLAYING), onIntent = {}) }

        onNodeWithText(text = "Pause").assertIsEnabled()
    }

    @Test
    fun `the transport button toggles playback`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { pane(video = loaded(), onIntent = { intents += it }) }

            onNodeWithText(text = "Play").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.TogglePlayback)
    }

    @Test
    fun `the position and the duration are rendered as timecodes`() = runComposeUiTest {
        setContent {
            pane(video = loaded(positionMillis = 65_400L, durationMillis = 125_000L), onIntent = {})
        }

        onNodeWithText(text = "1:05.4").assertIsDisplayed()
        onNodeWithText(text = "2:05.0").assertIsDisplayed()
    }

    @Test
    fun `a playback failure explains itself instead of pretending to decode`() = runComposeUiTest {
        val broken = VideoUiState(
            name = "clip.mp4",
            status = PlaybackStatus.ERROR,
            errorMessage = "libVLC was not found. Install VLC media player.",
        )

        setContent { pane(video = broken, onIntent = {}) }

        onNodeWithText(text = "Playback unavailable").assertIsDisplayed()
        onNodeWithText(text = broken.errorMessage.orEmpty()).assertIsDisplayed()
        onNodeWithText(text = "Play").assertIsNotEnabled()
    }

    @Test
    fun `opening a screencast is requested from the header`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { pane(video = loaded(), onIntent = { intents += it }) }

            onNodeWithText(text = "Replace…").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.RequestVideoImport)
    }

    private fun loaded(
        status: PlaybackStatus = PlaybackStatus.PAUSED,
        positionMillis: Long = 0L,
        durationMillis: Long = 60_000L,
    ) = VideoUiState(
        name = "clip.mp4",
        status = status,
        positionMillis = positionMillis,
        durationMillis = durationMillis,
    )

    @androidx.compose.runtime.Composable
    private fun pane(video: VideoUiState, onIntent: (LogPlayerIntent) -> Unit) {
        VideoPane(
            video = video,
            frame = mutableStateOf<VideoFrame?>(value = null),
            onIntent = onIntent,
        )
    }
}
