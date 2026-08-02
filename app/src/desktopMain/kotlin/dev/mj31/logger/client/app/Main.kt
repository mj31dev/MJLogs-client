package dev.mj31.logger.client.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mj31.logger.client.app.di.DesktopAppComponent
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.app_icon
import dev.mj31.logger.client.app.resources.app_window_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import dev.mj31.logger.client.app.di.create
import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import java.awt.Dimension

private const val MIN_WINDOW_WIDTH = 1100
private const val MIN_WINDOW_HEIGHT = 700

/**
 * Entry point of the desktop application.
 *
 * Files may also be passed on the command line, which makes the workspace scriptable and gives a
 * one-liner demo: `./gradlew :app:desktopRun` plus the paths of a screencast and of some log files.
 */
fun main(args: Array<String>) = application {
    val component = remember { DesktopAppComponent::class.create() }
    val windowState = rememberWindowState(size = DpSize(width = 1440.dp, height = 900.dp))

    DisposableEffect(key1 = component) {
        onDispose { component.dispose() }
    }

    LaunchedEffect(key1 = component) {
        openStartupFiles(paths = args.toList(), store = component.store)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(resource = Res.string.app_window_title),
        icon = painterResource(resource = Res.drawable.app_icon),
        state = windowState,
    ) {
        window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
        App(store = component.store, fileChooser = component.fileChooser)
    }
}

/**
 * Routes command line arguments by file type.
 *
 * Unsupported paths are still forwarded as logs on purpose: the import rejects them with a message,
 * which is more helpful than silently ignoring a file the user explicitly asked for.
 */
internal fun openStartupFiles(paths: List<String>, store: LogPlayerStore) {
    val videos = paths.filter { SupportedFileTypes.accepts(kind = MediaKind.VIDEO, path = it) }
    videos.firstOrNull()?.let { path -> store.handleIntent(intent = LogPlayerIntent.ImportVideo(path = path)) }
    store.handleIntent(intent = LogPlayerIntent.ImportLogFiles(paths = paths - videos.toSet()))
}
