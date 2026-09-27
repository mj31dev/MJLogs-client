package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.StartDayRequestUiState
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlinx.datetime.LocalDate
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class StartDayDialogTest {

    @Test
    fun `names the file and offers both days`() = runComposeUiTest {
        setContent { StartDayDialog(request = ambiguousFile(), onIntent = {}) }

        onNodeWithText(text = "night-shift.log").assertIsDisplayed()
        onNodeWithText(text = "Wednesday, 12 August 2026").assertIsDisplayed()
        onNodeWithText(text = "Tuesday, 11 August 2026").assertIsDisplayed()
    }

    @Test
    fun `neither day is chosen until the user chooses one`() = runComposeUiTest {
        setContent { StartDayDialog(request = ambiguousFile(), onIntent = {}) }

        onNodeWithText(text = "Use this day").assertIsNotEnabled()
    }

    @Test
    fun `choosing the earlier day reads the file under it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { StartDayDialog(request = ambiguousFile(), onIntent = { intents += it }) }

            onNodeWithText(text = "Tuesday, 11 August 2026").performClick()
            onNodeWithText(text = "Use this day").performClick()
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.ChooseStartDay(day = LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 11)),
        )
    }

    @Test
    fun `choosing the later day reads the file under it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { StartDayDialog(request = ambiguousFile(), onIntent = { intents += it }) }

            onNodeWithText(text = "Wednesday, 12 August 2026").performClick()
            onNodeWithText(text = "Use this day").performClick()
        }

        assertThat(intents).containsExactly(
            LogPlayerIntent.ChooseStartDay(day = LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 12)),
        )
    }

    @Test
    fun `skipping leaves the file out of the session`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { StartDayDialog(request = ambiguousFile(), onIntent = { intents += it }) }

            onNodeWithText(text = "Skip file").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.DismissStartDayRequest)
    }

    private fun ambiguousFile(): StartDayRequestUiState = StartDayRequestUiState(
        path = "/captures/2026-08-12/night-shift.log",
        fileName = "night-shift.log",
        candidates = listOf(
            LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 12),
            LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 11),
        ),
        format = LogFormatSpec.Regex(
            name = "time only",
            linePattern = "(?<ts>\\d{2}:\\d{2}:\\d{2}) (?<message>.*)",
            timestampPattern = "HH:mm:ss",
        ),
    )
}
