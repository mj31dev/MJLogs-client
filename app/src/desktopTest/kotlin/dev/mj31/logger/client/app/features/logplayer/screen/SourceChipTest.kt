package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SourceChipTest {

    @Test
    fun `the header of a file opens from its chip menu`() {
        val intents = mutableListOf<LogPlayerIntent>()
        var toggled = 0

        runComposeUiTest {
            setContent {
                SourceChip(source = source(hasPreamble = true), onClick = { toggled++ }, onIntent = { intents += it })
            }

            onNodeWithTag(testTag = "$SOURCE_CHIP_MENU_TAG-src-1").performClick()
            onNodeWithTag(testTag = "$SOURCE_MENU_PREAMBLE_TAG-src-1").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ShowSourcePreamble(sourceId = "src-1"))
        assertThat(toggled).isEqualTo(0)
    }

    @Test
    fun `a file without a header whose lines fix its zone offers no menu`() = runComposeUiTest {
        val source = source(hasPreamble = false).copy(zone = SourceZone(id = "UTC+03:00", origin = ZoneOrigin.LINE))
        setContent { SourceChip(source = source, onClick = {}, onIntent = {}) }

        onNodeWithTag(testTag = "$SOURCE_CHIP_MENU_TAG-src-1").assertDoesNotExist()
    }

    @Test
    fun `clicking the chip still shows or hides the file`() {
        var toggled = 0

        runComposeUiTest {
            setContent { SourceChip(source = source(hasPreamble = true), onClick = { toggled++ }, onIntent = {}) }

            onNodeWithText(text = "device.log").performClick()
        }

        assertThat(toggled).isEqualTo(1)
    }

    private fun source(hasPreamble: Boolean): LogSourceUi = LogSourceUi(
        id = "src-1",
        name = "device.log",
        formatName = "Logcat",
        entryCount = 3,
        skippedLineCount = 0,
        isSelected = true,
        hasPreamble = hasPreamble,
    )
}
