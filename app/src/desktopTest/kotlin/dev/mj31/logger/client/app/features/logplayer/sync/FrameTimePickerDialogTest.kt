package dev.mj31.logger.client.app.features.logplayer.sync

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FrameTimePickerDialogTest {

    @Test
    fun `the dialog opens on the moment it was given`() = runComposeUiTest {
        setContent {
            FrameTimePickerDialog(initial = MOMENT, onDismiss = {}, onConfirm = { _, _, _ -> })
        }

        onNodeWithText(text = "Time shown in this frame").assertIsDisplayed()
        onNodeWithText(text = "Pick the date and the hour; the seconds already typed are kept.").assertIsDisplayed()
    }

    @Test
    fun `confirming reports the picked day together with the picked hour and minute`() {
        var picked: Triple<Long, Int, Int>? = null

        runComposeUiTest {
            setContent {
                FrameTimePickerDialog(
                    initial = MOMENT,
                    onDismiss = {},
                    onConfirm = { dateMillis, hour, minute -> picked = Triple(dateMillis, hour, minute) },
                )
            }

            onNodeWithText(text = "OK").performClick()
        }

        // The picker reports the day itself, at midnight UTC; the finer part stays in the field.
        assertThat(picked).isEqualTo(Triple(DAY_MILLIS, 18, 50))
    }

    @Test
    fun `cancelling reports nothing`() {
        var dismissed = false
        var picked = false

        runComposeUiTest {
            setContent {
                FrameTimePickerDialog(
                    initial = MOMENT,
                    onDismiss = { dismissed = true },
                    onConfirm = { _, _, _ -> picked = true },
                )
            }

            onNodeWithText(text = "Cancel").performClick()
        }

        assertThat(dismissed).isTrue()
        assertThat(picked).isFalse()
    }

    private companion object {
        val MOMENT: Instant = Instant.parse("2026-06-29T18:50:07.267Z")

        val DAY_MILLIS: Long = Instant.parse("2026-06-29T00:00:00Z").toEpochMilliseconds()
    }
}
