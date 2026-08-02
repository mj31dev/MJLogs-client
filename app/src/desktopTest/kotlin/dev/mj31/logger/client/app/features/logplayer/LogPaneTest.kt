package dev.mj31.logger.client.app.features.logplayer

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.screen.LogPane

@OptIn(ExperimentalTestApi::class)
class LogPaneTest {

    @Test
    fun `renders every visible record and the session counts`() = runComposeUiTest {
        setContent { LogPane(state = loadedState(), onIntent = {}) }

        onNodeWithText(text = "connected to server").assertIsDisplayed()
        onNodeWithText(text = "write failed").assertIsDisplayed()
        onNodeWithText(text = "2 records").assertIsDisplayed()
        onNodeWithText(text = "app.txt").assertIsDisplayed()
    }

    @Test
    fun `clicking a record selects it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { LogPane(state = loadedState(), onIntent = { intents += it }) }

            onNodeWithText(text = "write failed").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SelectEntry(entryId = "e2"))
    }

    @Test
    fun `the add button asks for the native picker`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { LogPane(state = loadedState(), onIntent = { intents += it }) }

            onNodeWithText(text = "Add logs…").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.RequestLogImport)
    }

    @Test
    fun `hiding a level narrows the filter to the remaining ones`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            // An empty session keeps the level chips the only single letter nodes on screen.
            setContent { LogPane(state = LogPlayerState(), onIntent = { intents += it }) }

            onNodeWithText(text = "E").performClick()
        }

        val filter = (intents.single() as LogPlayerIntent.UpdateFilter).filter
        assertThat(filter.levels).containsExactlyElementsIn(LogLevel.entries.toSet() - LogLevel.ERROR)
    }

    @Test
    fun `the list scrolls to the record under the playhead`() = runComposeUiTest {
        setContent { LogPane(state = longSessionState(activeEntryId = "e150", followVideo = true), onIntent = {}) }

        onNodeWithText(text = "record number 150").assertIsDisplayed()
    }

    @Test
    fun `the list stays put when following the video is off`() = runComposeUiTest {
        setContent { LogPane(state = longSessionState(activeEntryId = "e150", followVideo = false), onIntent = {}) }

        onNodeWithText(text = "record number 1").assertIsDisplayed()
        onNodeWithText(text = "record number 150").assertDoesNotExist()
    }

    @Test
    fun `hiding a source keeps the others visible`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = loadedState().copy(sources = uiSources + uiSources.first().copy(id = "src-2", name = "network.txt"))

        runComposeUiTest {
            setContent { LogPane(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "app.txt").performClick()
        }

        val filter = (intents.single() as LogPlayerIntent.UpdateFilter).filter
        assertThat(filter.sourceIds).containsExactly("src-2")
    }

    @Test
    fun `a source chip shows what was parsed out of the file`() = runComposeUiTest {
        val state = loadedState().copy(sources = uiSources.map { it.copy(skippedLineCount = 3) })

        setContent { LogPane(state = state, onIntent = {}) }

        onNodeWithText(text = "2 records · ISO-8601, 3 skipped").assertIsDisplayed()
    }

    @Test
    fun `an empty session explains how to add files`() = runComposeUiTest {
        setContent { LogPane(state = LogPlayerState(), onIntent = {}) }

        onNodeWithText(text = "No log files loaded").assertIsDisplayed()
    }

    @Test
    fun `filters that hide everything offer a reset`() {
        val intents = mutableListOf<LogPlayerIntent>()
        val state = LogPlayerState(
            sources = uiSources,
            entries = emptyList(),
            totalEntryCount = uiEntries.size,
            filter = LogFilter(query = "nothing matches this"),
        )

        runComposeUiTest {
            setContent { LogPane(state = state, onIntent = { intents += it }) }

            onNodeWithText(text = "No record matches the filters").assertIsDisplayed()
            onNodeWithText(text = "Reset filters").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.UpdateFilter(filter = LogFilter()))
    }
}
