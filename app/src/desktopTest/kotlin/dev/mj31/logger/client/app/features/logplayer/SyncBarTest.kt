package dev.mj31.logger.client.app.features.logplayer

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.test.Test
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.SyncUiState
import dev.mj31.logger.client.app.features.logplayer.screen.SyncBar

@OptIn(ExperimentalTestApi::class)
class SyncBarTest {

    @Test
    fun `independent timelines cannot be synchronized without a selected record`() = runComposeUiTest {
        setContent { SyncBar(state = loadedState(), onIntent = {}) }

        onNodeWithText(text = "Timelines independent").assertIsDisplayed()
        onNodeWithText(text = "Synchronize").assertIsNotEnabled()
        onNode(matcher = isToggleable()).assertIsNotEnabled()
    }

    @Test
    fun `a selected record and a screencast unlock the synchronization`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(sync = SyncUiState(canSynchronize = true))

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "Synchronize").assertIsEnabled().performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.Synchronize)
    }

    @Test
    fun `a synchronized session can be unlinked`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(sync = SyncUiState(isSynced = true, canSynchronize = true))

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "Timelines synchronized").assertIsDisplayed()
            onNodeWithText(text = "Re-sync here").assertIsDisplayed()
            onNodeWithText(text = "Unlink").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ClearSynchronization)
    }

    @Test
    fun `an independent bar explains what the user has to do`() = runComposeUiTest {
        setContent { SyncBar(state = loadedState(), onIntent = {}) }

        onNodeWithText(text = "Select a log record, move the playhead, then press Synchronize").assertIsDisplayed()
    }

    @Test
    fun `a synchronized bar reports the mapping and the covered range`() = runComposeUiTest {
        val anchor = SyncAnchor(
            logTimestamp = LogPlayerFixtures.at(offsetMillis = 0L),
            videoPositionMillis = 0L,
            origin = SyncOrigin.SELECTED_ENTRY,
            logEntryId = "e1",
        )
        val state = loadedState().copy(
            video = VideoUiState(name = "clip.mp4", positionMillis = 5_000L, durationMillis = 60_000L),
            sync = SyncUiState(
                isSynced = true,
                origin = SyncOrigin.SELECTED_ENTRY,
                logTimeAtPlayhead = LogPlayerFixtures.at(offsetMillis = 5_000L),
                overlap = ResolveTimelineOverlapUseCase()(
                    logRange = TimeRange(
                        start = LogPlayerFixtures.at(offsetMillis = 0L),
                        end = LogPlayerFixtures.at(offsetMillis = 10_000L),
                    ),
                    anchor = anchor,
                    videoDurationMillis = 60_000L,
                ),
            ),
        )

        setContent { SyncBar(state = state, onIntent = {}) }

        onNodeWithText(
            text = "video 0:05.0 = log 10:00:05.000  |  logs covered 10:00:00.000 .. 10:00:10.000" +
                "  |  by the selected record",
        ).assertIsDisplayed()
    }

    @Test
    fun `following the video can be turned off once synchronized`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(sync = SyncUiState(isSynced = true), followVideo = true)

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNode(matcher = isToggleable()).assertIsEnabled().performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SetFollowVideo(enabled = false))
    }

    @Test
    fun `the frame time field reports every keystroke as an intent`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { SyncBar(state = loadedState(), onIntent = { intents += it }) }

            onNodeWithText(text = "Time on this frame").performTextInput(text = "10:00:20")
        }

        assertThat(intents.last()).isEqualTo(LogPlayerIntent.UpdateFrameTime(text = "10:00:20"))
    }

    @Test
    fun `a typed frame time can be applied without selecting a record`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(
            sync = SyncUiState(frameTime = "10:00:20", canSynchronizeAtFrameTime = true),
        )

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "Use this time").assertIsEnabled().performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SynchronizeAtFrameTime)
    }

    @Test
    fun `an empty frame time leaves the action disabled`() = runComposeUiTest {
        setContent { SyncBar(state = loadedState(), onIntent = {}) }

        onNodeWithText(text = "Use this time").assertIsNotEnabled()
    }

    @Test
    fun `a time that could not be read is explained on the field`() = runComposeUiTest {
        val state = loadedState().copy(
            sync = SyncUiState(frameTime = "at some point", frameTimeError = true, canSynchronizeAtFrameTime = true),
        )

        setContent { SyncBar(state = state, onIntent = {}) }

        onNodeWithText(text = "Enter a time such as 2026-06-29 18:50:07.267 or 18:50:07").assertIsDisplayed()
    }

    @Test
    fun `the picker opens from the bar and writes its choice back as an intent`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(
            sync = SyncUiState(frameTimeDefault = LogPlayerFixtures.at(offsetMillis = 0L)),
        )

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "Pick…").performClick()
            onNodeWithText(text = "Time shown in this frame").assertIsDisplayed()
            onNodeWithText(text = "OK").performClick()
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.PickFrameTime(dateMillis = DAY_MILLIS, hour = 10, minute = 0),
        )
    }

    @Test
    fun `cancelling the picker leaves the field untouched`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(
            sync = SyncUiState(frameTime = "10:00:20", frameTimeDefault = LogPlayerFixtures.at(offsetMillis = 0L)),
        )

        runComposeUiTest {
            setContent { SyncBar(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "Pick…").performClick()
            onNodeWithText(text = "Cancel").performClick()
        }

        assertThat(intents).isEmpty()
    }

    private companion object {
        /** Midnight UTC of the fixture session day, which is what the date picker reports. */
        val DAY_MILLIS: Long = LogPlayerFixtures.BASE.toEpochMilliseconds() - 10 * 60 * 60 * 1_000L
    }
}
