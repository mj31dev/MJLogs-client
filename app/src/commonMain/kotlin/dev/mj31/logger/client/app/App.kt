package dev.mj31.logger.client.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.app.features.sessions.SessionsIntent
import dev.mj31.logger.client.app.features.sessions.SessionsScreen
import dev.mj31.logger.client.app.features.sessions.SessionsStore
import dev.mj31.logger.client.app.platform.FileChooser
import dev.mj31.logger.client.app.theme.LoggerTheme
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import dev.mj31.logger.client.app.view.UiMessage
import kotlinx.coroutines.delay
import dev.mj31.logger.client.app.features.logplayer.screen.PlayerScreen

private const val MESSAGE_TIMEOUT_MILLIS = 6_000L

/** What the save panel opens on before the workspace has a name of its own. */
private const val DEFAULT_SESSION_NAME = "session"

/**
 * Wires the MVI cycles to the platform: state and effects flow out of the stores, intents flow back.
 *
 * Native file dialogs live here rather than in a store, which is why they are requested through
 * [LogPlayerEffect.PickVideoFile] and its siblings.
 *
 * The two screens are separate contours that meet here, and only here: every action of the session
 * list that changes what is open is turned into a player intent, because the workspace belongs to
 * the player. The list itself only ever forgets an entry.
 */
@Composable
fun App(
    store: LogPlayerStore,
    sessionsStore: SessionsStore,
    fileChooser: FileChooser,
    screen: AppScreen,
    onScreen: (AppScreen) -> Unit,
    themeChoice: ThemeChoice,
) {
    LoggerTheme(choice = themeChoice) {
        val state by store.state.collectAsState()
        val sessionsState by sessionsStore.state.collectAsState()
        val frame = store.frames.collectAsState()
        var message by remember { mutableStateOf<UiMessage?>(value = null) }

        LaunchedEffect(key1 = store, key2 = fileChooser) {
            store.effects.collect { effect ->
                handleEffect(
                    effect = effect,
                    store = store,
                    fileChooser = fileChooser,
                    suggestedName = state.workspace.packageName ?: DEFAULT_SESSION_NAME,
                    onMessage = { message = it },
                    onScreen = onScreen,
                )
            }
        }

        LaunchedEffect(key1 = message) {
            if (message != null) {
                delay(timeMillis = MESSAGE_TIMEOUT_MILLIS)
                message = null
            }
        }

        // The last workspace is a single stored row, not a stream. Coming back to the list is
        // exactly when its description can have gone stale, so that is when it is re-read.
        LaunchedEffect(key1 = screen) {
            if (screen == AppScreen.SESSIONS) sessionsStore.refresh()
        }

        when (screen) {
            AppScreen.SESSIONS -> SessionsScreen(
                state = sessionsState,
                onIntent = { intent ->
                    routeSessionsIntent(
                        intent = intent,
                        store = store,
                        sessionsStore = sessionsStore,
                        onScreen = onScreen,
                    )
                },
            )

            AppScreen.PLAYER -> PlayerScreen(
                state = state,
                frame = frame,
                message = message,
                onIntent = store::handleIntent,
                onDismissMessage = { message = null },
            )
        }
    }
}

/**
 * Answers a one-shot request from the store, which for most of them means a native dialog.
 *
 * A dialog is the one place the user can decline, so nothing here acts on a `null`: cancelling has
 * to leave the workspace and the screen exactly as they were.
 */
private suspend fun handleEffect(
    effect: LogPlayerEffect,
    store: LogPlayerStore,
    fileChooser: FileChooser,
    suggestedName: String,
    onMessage: (UiMessage) -> Unit,
    onScreen: (AppScreen) -> Unit,
) {
    when (effect) {
        is LogPlayerEffect.ShowMessage ->
            onMessage(UiMessage(text = effect.text, isError = effect.isError))

        LogPlayerEffect.PickVideoFile -> fileChooser.chooseVideo()?.let { path ->
            store.handleIntent(intent = LogPlayerIntent.ImportVideo(path = path))
        }

        LogPlayerEffect.PickLogFiles -> store.handleIntent(
            intent = LogPlayerIntent.ImportLogFiles(paths = fileChooser.chooseLogFiles()),
        )

        LogPlayerEffect.PickSessionFile -> fileChooser.chooseSessionFile()?.let { path ->
            store.handleIntent(intent = LogPlayerIntent.OpenSession(path = path))
            onScreen(AppScreen.PLAYER)
        }

        LogPlayerEffect.PickSessionSaveTarget -> fileChooser.chooseSessionTarget(
            suggestedName = suggestedName,
        )?.let { path ->
            store.handleIntent(intent = LogPlayerIntent.SaveSession(path = path))
        }
    }
}

/**
 * Turns a choice made on the start screen into the player action it stands for.
 *
 * Everything except forgetting an entry ends in the player, because everything except forgetting an
 * entry replaces the workspace.
 */
private fun routeSessionsIntent(
    intent: SessionsIntent,
    store: LogPlayerStore,
    sessionsStore: SessionsStore,
    onScreen: (AppScreen) -> Unit,
) {
    when (intent) {
        is SessionsIntent.Open -> {
            store.handleIntent(intent = LogPlayerIntent.OpenSession(path = intent.path))
            onScreen(AppScreen.PLAYER)
        }

        SessionsIntent.ContinueLast -> {
            store.handleIntent(intent = LogPlayerIntent.ContinueLastSession)
            onScreen(AppScreen.PLAYER)
        }

        SessionsIntent.StartNew -> {
            store.handleIntent(intent = LogPlayerIntent.StartNewSession)
            onScreen(AppScreen.PLAYER)
        }

        // The screen moves when the dialog comes back with a path, not when it opens.
        SessionsIntent.RequestOpenFile -> store.handleIntent(intent = LogPlayerIntent.RequestOpenSession)

        is SessionsIntent.Forget -> sessionsStore.handleIntent(intent = intent)
    }
}
