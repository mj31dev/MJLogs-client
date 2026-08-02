package dev.mj31.logger.client.app.view

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.view.LogRow
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel

@OptIn(ExperimentalTestApi::class)
class LogRowTest {

    private val entry = LogPlayerFixtures.entry(
        id = "e1",
        offsetMillis = 0L,
        level = LogLevel.WARN,
        tag = "CacheStore",
        message = "evicted 15 entries",
    )

    @Test
    fun `renders the time, the level initial, the tag and the message`() = runComposeUiTest {
        setContent {
            LogRow(entry = entry, isSelected = false, isActive = false, isAnchor = false, onClick = {})
        }

        onNodeWithText(text = "10:00:00.000").assertIsDisplayed()
        onNodeWithText(text = "W").assertIsDisplayed()
        onNodeWithText(text = "CacheStore").assertIsDisplayed()
        onNodeWithText(text = "evicted 15 entries").assertIsDisplayed()
    }

    @Test
    fun `a multi line message keeps its continuation lines`() = runComposeUiTest {
        val withStackTrace = entry.copy(message = "upload failed\n    at Http2Stream.takeHeaders")

        setContent {
            LogRow(entry = withStackTrace, isSelected = false, isActive = false, isAnchor = false, onClick = {})
        }

        onNodeWithText(text = withStackTrace.message).assertIsDisplayed()
    }

    @Test
    fun `the row reports a click whatever its visual state`() {
        val clicks = mutableListOf<String>()

        runComposeUiTest {
            setContent {
                LogRow(
                    entry = entry,
                    isSelected = true,
                    isActive = true,
                    isAnchor = true,
                    onClick = { clicks += entry.id },
                )
            }

            onNodeWithText(text = "evicted 15 entries").performClick()
        }

        assertThat(clicks).containsExactly("e1")
    }

    @Test
    fun `an empty tag does not break the layout`() = runComposeUiTest {
        setContent {
            LogRow(
                entry = entry.copy(tag = ""),
                isSelected = false,
                isActive = false,
                isAnchor = false,
                onClick = {},
            )
        }

        onNodeWithText(text = "evicted 15 entries").assertIsDisplayed()
    }
}
