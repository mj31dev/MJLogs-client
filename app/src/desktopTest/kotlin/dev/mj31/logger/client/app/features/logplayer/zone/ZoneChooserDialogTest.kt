package dev.mj31.logger.client.app.features.logplayer.zone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.domain.model.time.SourceZone
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ZoneChooserDialogTest {

    private val request = ZoneRequestUiState(
        target = ZoneTarget.Source(sourceId = "src-1"),
        subject = "device.log",
        current = SourceZone.UTC,
        availableZoneIds = listOf("Asia/Tokyo", "Europe/Berlin", "Europe/London"),
    )

    @Test
    fun `names the file the zone is chosen for`() = runComposeUiTest {
        setContent { ZoneChooserDialog(request = request, onIntent = {}) }

        onNodeWithText(text = "device.log").assertIsDisplayed()
    }

    @Test
    fun `picking a region applies it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { ZoneChooserDialog(request = request, onIntent = { intents += it }) }

            onNodeWithTag(testTag = "$ZONE_OPTION_TAG-Europe/Berlin").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ChooseZone(zoneId = "Europe/Berlin"))
    }

    @Test
    fun `a typed offset is offered in the form it is stored in`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { ZoneChooserDialog(request = request, onIntent = { intents += it }) }

            onNodeWithTag(testTag = ZONE_SEARCH_TAG).performTextInput(text = "utc+3")
            onNodeWithTag(testTag = "$ZONE_OPTION_TAG-UTC+03:00").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ChooseZone(zoneId = "UTC+03:00"))
    }

    @Test
    fun `searching narrows the list to matching regions`() = runComposeUiTest {
        setContent { ZoneChooserDialog(request = request, onIntent = {}) }

        onNodeWithTag(testTag = ZONE_SEARCH_TAG).performTextInput(text = "tok")

        onNodeWithTag(testTag = "$ZONE_OPTION_TAG-Asia/Tokyo").assertIsDisplayed()
        onNodeWithTag(testTag = "$ZONE_OPTION_TAG-Europe/Berlin").assertDoesNotExist()
    }

    @Test
    fun `the automatic row hands the decision back to the file`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { ZoneChooserDialog(request = request, onIntent = { intents += it }) }

            onNodeWithTag(testTag = ZONE_AUTOMATIC_TAG).performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ChooseZone(zoneId = null))
    }

    @Test
    fun `cancelling changes nothing`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { ZoneChooserDialog(request = request, onIntent = { intents += it }) }

            onNodeWithTag(testTag = ZONE_DISMISS_TAG).performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.DismissZoneRequest)
    }
}
