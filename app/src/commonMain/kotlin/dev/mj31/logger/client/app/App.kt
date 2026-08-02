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
import dev.mj31.logger.client.app.platform.FileChooser
import dev.mj31.logger.client.app.theme.LoggerTheme
import dev.mj31.logger.client.app.view.UiMessage
import kotlinx.coroutines.delay
import dev.mj31.logger.client.app.features.logplayer.screen.PlayerScreen

private const val MESSAGE_TIMEOUT_MILLIS = 6_000L

/**
 * Wires the MVI cycle to the platform: state and effects flow out of the store, intents flow back in.
 *
 * Native file dialogs live here rather than in the store, which is why they are requested through
 * [LogPlayerEffect.PickVideoFile] and [LogPlayerEffect.PickLogFiles].
 */
@Composable
fun App(
    store: LogPlayerStore,
    fileChooser: FileChooser,
) {
    LoggerTheme {
        val state by store.state.collectAsState()
        val frame = store.frames.collectAsState()
        var message by remember { mutableStateOf<UiMessage?>(value = null) }

        LaunchedEffect(key1 = store, key2 = fileChooser) {
            store.effects.collect { effect ->
                when (effect) {
                    is LogPlayerEffect.ShowMessage ->
                        message = UiMessage(text = effect.text, isError = effect.isError)

                    LogPlayerEffect.PickVideoFile -> fileChooser.chooseVideo()?.let { path ->
                        store.handleIntent(intent = LogPlayerIntent.ImportVideo(path = path))
                    }

                    LogPlayerEffect.PickLogFiles -> store.handleIntent(
                        intent = LogPlayerIntent.ImportLogFiles(paths = fileChooser.chooseLogFiles()),
                    )
                }
            }
        }

        LaunchedEffect(key1 = message) {
            if (message != null) {
                delay(timeMillis = MESSAGE_TIMEOUT_MILLIS)
                message = null
            }
        }

        PlayerScreen(
            state = state,
            frame = frame,
            message = message,
            onIntent = store::handleIntent,
            onDismissMessage = { message = null },
        )
    }
}
