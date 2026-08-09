package dev.mj31.logger.client.app.features.sessions

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The list of saved sessions.
 *
 * Every assertion is about what the row lets the user do, because the list is only useful for the
 * two verbs it carries: open this one, stop showing me that one.
 */
@OptIn(ExperimentalTestApi::class)
class SessionsScreenTest {

    @Test
    fun `an empty list says so and still offers the picker`() = runComposeUiTest {
        setContent {
            SessionsScreen(state = SessionsState(), onIntent = {})
        }

        onNodeWithText(text = "No session files have been opened yet").assertIsDisplayed()
        onNodeWithText(text = "Open a session file…").assertIsDisplayed()
    }

    /** A first run has nothing to continue and nothing saved, and must still lead somewhere. */
    @Test
    fun `starting fresh is offered even with nothing to show`() {
        val intents = mutableListOf<SessionsIntent>()

        runComposeUiTest {
            setContent {
                SessionsScreen(state = SessionsState(), onIntent = { intents += it })
            }

            onNodeWithText(text = "New session").performClick()
        }

        assertThat(intents).containsExactly(SessionsIntent.StartNew)
    }

    @Test
    fun `the last workspace is offered above the saved files`() = runComposeUiTest {
        setContent {
            SessionsScreen(
                state = SessionsState(lastSession = lastSession(), recent = listOf(full())),
                onIntent = {},
            )
        }

        onNodeWithText(text = "Continue where you left off").assertIsDisplayed()
        onNodeWithText(text = "run.mp4").assertIsDisplayed()
        onNodeWithText(text = "2 log files", substring = true).assertIsDisplayed()
    }

    @Test
    fun `continuing is one click on the card`() {
        val intents = mutableListOf<SessionsIntent>()

        runComposeUiTest {
            setContent {
                SessionsScreen(state = SessionsState(lastSession = lastSession()), onIntent = { intents += it })
            }

            onNodeWithText(text = "run.mp4").performClick()
        }

        assertThat(intents).containsExactly(SessionsIntent.ContinueLast)
    }

    /** Nothing was open last time, so there is nothing to carry on from. */
    @Test
    fun `a first run does not offer to continue`() = runComposeUiTest {
        setContent {
            SessionsScreen(state = SessionsState(recent = listOf(full())), onIntent = {})
        }

        onNodeWithText(text = "Continue where you left off").assertDoesNotExist()
    }

    /**
     * The row used to say which of two shapes the file had.
     *
     * There is one shape now, so the badge went with it: a label that is the same on every row
     * carries no information and only competes with the name.
     */
    @Test
    fun `a row names the session and where it lives`() = runComposeUiTest {
        setContent {
            SessionsScreen(state = SessionsState(recent = listOf(full(), light())), onIntent = {})
        }

        onNodeWithText(text = "investigation").assertIsDisplayed()
        onNodeWithText(text = "/cases/investigation.mjclog", substring = true).assertIsDisplayed()
        onNodeWithText(text = "glance").assertIsDisplayed()
    }

    /**
     * The row once reused the log-record format and printed `… 06:53:20.000`.
     *
     * Nothing asserted on it, and nothing was wrong in any behavioural sense — it simply read as
     * noise next to the file name, which is what looking at the rendered screen showed.
     */
    @Test
    fun `the row does not print the millisecond tail of a log timestamp`() = runComposeUiTest {
        setContent {
            SessionsScreen(state = SessionsState(recent = listOf(full())), onIntent = {})
        }

        onNodeWithText(text = ".000", substring = true).assertDoesNotExist()
    }

    /** The row is the button: one file, one verb, no need for a control that repeats the verb. */
    @Test
    fun `clicking a row opens that session`() {
        val intents = mutableListOf<SessionsIntent>()

        runComposeUiTest {
            setContent {
                SessionsScreen(state = SessionsState(recent = listOf(full())), onIntent = { intents += it })
            }

            onNodeWithText(text = "investigation").performClick()
        }

        assertThat(intents).containsExactly(SessionsIntent.Open(path = "/cases/investigation.mjclog"))
    }

    @Test
    fun `forgetting an entry does not open it`() {
        val intents = mutableListOf<SessionsIntent>()

        runComposeUiTest {
            setContent {
                SessionsScreen(state = SessionsState(recent = listOf(full())), onIntent = { intents += it })
            }

            onNodeWithText(text = "Remove").performClick()
        }

        assertThat(intents).containsExactly(SessionsIntent.Forget(path = "/cases/investigation.mjclog"))
    }

    @Test
    fun `the picker is offered even when the list is full`() {
        val intents = mutableListOf<SessionsIntent>()

        runComposeUiTest {
            setContent {
                SessionsScreen(
                    state = SessionsState(recent = listOf(full(), light())),
                    onIntent = { intents += it },
                )
            }

            onNodeWithText(text = "Open a session file…").performClick()
        }

        assertThat(intents).containsExactly(SessionsIntent.RequestOpenFile)
    }

    private fun lastSession(): LastSessionUi = LastSessionUi(
        label = "run.mp4",
        logCount = 2,
        hasVideo = true,
    )

    private fun full(): RecentPackage = RecentPackage(
        path = "/cases/investigation.mjclog",
        name = "investigation",
        lastOpened = Instant.fromEpochMilliseconds(epochMilliseconds = 1_700_000_000_000L),
    )

    private fun light(): RecentPackage = RecentPackage(
        path = "/cases/glance.mjclog",
        name = "glance",
        lastOpened = Instant.fromEpochMilliseconds(epochMilliseconds = 1_700_000_100_000L),
    )
}
