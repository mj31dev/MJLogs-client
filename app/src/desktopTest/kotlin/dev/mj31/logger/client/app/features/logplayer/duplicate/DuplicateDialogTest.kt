package dev.mj31.logger.client.app.features.logplayer.duplicate

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateChoice
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateKind
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateRequestUiState
import dev.mj31.logger.client.domain.model.log.LogSource
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DuplicateDialogTest {

    @Test
    fun `an overlap offers merging, opening separately and skipping`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { DuplicateDialog(request = request(kind = DuplicateKind.OVERLAP), onIntent = { intents += it }) }

            onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.ADD_SEPARATELY}").assertIsDisplayed()
            onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.SKIP}").assertIsDisplayed()
            onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.MERGE}").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ResolveDuplicate(choice = DuplicateChoice.MERGE))
    }

    @Test
    fun `a copy offers no merge`() = runComposeUiTest {
        setContent { DuplicateDialog(request = request(kind = DuplicateKind.COPY), onIntent = {}) }

        onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.MERGE}").assertDoesNotExist()
        onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.SKIP}").assertIsDisplayed()
    }

    @Test
    fun `opening a copy anyway is a deliberate choice`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { DuplicateDialog(request = request(kind = DuplicateKind.COPY), onIntent = { intents += it }) }

            onNodeWithTag(testTag = "$DUPLICATE_CHOICE_TAG-${DuplicateChoice.ADD_SEPARATELY}").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.ResolveDuplicate(choice = DuplicateChoice.ADD_SEPARATELY))
    }

    @Test
    fun `both files are shown with their folders`() = runComposeUiTest {
        setContent { DuplicateDialog(request = request(kind = DuplicateKind.COPY), onIntent = {}) }

        onNodeWithText(text = "/backup", substring = true).assertIsDisplayed()
        onNodeWithText(text = "/logs", substring = true).assertIsDisplayed()
    }

    private fun request(kind: DuplicateKind): DuplicateRequestUiState = DuplicateRequestUiState(
        kind = kind,
        candidate = LogSource(
            id = "src-9",
            name = "app.txt",
            path = "/backup/app.txt",
            format = LogPlayerFixtures.FIRST_SPEC,
            entries = emptyList(),
            referenceDate = LogPlayerFixtures.REFERENCE_DATE,
        ),
        existingSourceId = "src-1",
        existingName = "app.txt",
        existingPath = "/logs/app.txt",
        sharedRecordCount = 3,
    )
}
