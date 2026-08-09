package dev.mj31.logger.client.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.ApplicationScope
import dev.mj31.logger.client.app.di.DesktopAppComponent
import dev.mj31.logger.client.app.platform.DocumentOpenRequests
import dev.mj31.logger.client.app.features.legal.AboutWindow
import dev.mj31.logger.client.app.features.logplayer.state.ui.WorkspaceUiState
import dev.mj31.logger.client.app.features.sessions.SessionMenu
import dev.mj31.logger.client.app.features.sessions.ViewMenu
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.session.SessionFile
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.app_icon
import dev.mj31.logger.client.app.resources.app_window_title
import dev.mj31.logger.client.app.resources.legal_menu
import dev.mj31.logger.client.app.resources.legal_menu_item
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import dev.mj31.logger.client.app.di.create
import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.Dimension

private const val MIN_WINDOW_WIDTH = 1100
private const val MIN_WINDOW_HEIGHT = 700

/**
 * Whether the platform keeps an "About" entry in an application wide menu, as macOS does.
 *
 * That entry is where a user looks for what the application is and what it is made of, so the
 * licence texts answer it there instead of from a menu invented for them.
 */
private val hasApplicationAboutMenu: Boolean =
    Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)

/**
 * Entry point of the desktop application.
 *
 * Files may also be passed on the command line, which makes the workspace scriptable and gives a
 * one-liner demo: `./gradlew :app:desktopRun` plus the paths of a screencast and of some log files.
 */
fun main(args: Array<String>) {
    // Before anything else: macOS delivers the document that launched the application exactly once,
    // and a handler registered after that has already missed it.
    DocumentOpenRequests.install()
    application { MjLogsApplication(args = args) }
}

@Composable
private fun ApplicationScope.MjLogsApplication(args: Array<String>) {
    val component = remember { DesktopAppComponent::class.create() }
    val windowState = rememberWindowState(size = DpSize(width = 1440.dp, height = 900.dp))
    var showAbout by remember { mutableStateOf(value = false) }

    // A launch lands on the session list. Files named on the command line are a statement about what
    // should be open, so they skip it and go straight to the workspace.
    var screen by remember {
        mutableStateOf(value = if (args.isEmpty()) AppScreen.SESSIONS else AppScreen.PLAYER)
    }

    DisposableEffect(key1 = component) {
        onDispose { component.dispose() }
    }

    RegisterAboutHandler(onAbout = { showAbout = true })

    RegisterDocumentOpenHandler(
        component = component,
        onOpened = { screen = AppScreen.PLAYER },
    )

    LaunchedEffect(key1 = component) {
        openStartupFiles(paths = args.toList(), store = component.store)
    }

    val playerState by component.store.state.collectAsState()
    val sessionsState by component.sessionsStore.state.collectAsState()
    val themeChoice by component.preferences.themeChoice.collectAsState(initial = ThemeChoice.SYSTEM)
    val applicationScope = component.applicationScope

    Window(
        onCloseRequest = ::exitApplication,
        title = windowTitle(
            base = stringResource(resource = Res.string.app_window_title),
            sessionName = playerState.workspace.packageName,
            hasUnsavedChanges = playerState.workspace.hasUnsavedChanges,
        ),
        icon = painterResource(resource = Res.drawable.app_icon),
        state = windowState,
    ) {
        window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)

        AppMenuBar(
            workspace = playerState.workspace,
            recent = sessionsState.recent,
            themeChoice = themeChoice,
            onIntent = component.store::handleIntent,
            onScreen = { screen = it },
            onThemeChoice = { chosen ->
                applicationScope.launch { component.preferences.setThemeChoice(choice = chosen) }
            },
            onAbout = { showAbout = true },
        )

        App(
            store = component.store,
            sessionsStore = component.sessionsStore,
            fileChooser = component.fileChooser,
            screen = screen,
            onScreen = { screen = it },
            themeChoice = themeChoice,
        )
    }

    if (showAbout) {
        AboutWindow(
            readLegalNotices = component.readLegalNotices,
            onCloseRequest = { showAbout = false },
        )
    }
}

/**
 * The window menus.
 *
 * Every entry that opens something ends on the player, because every one of them replaces what the
 * workspace holds.
 */
@Composable
private fun FrameWindowScope.AppMenuBar(
    workspace: WorkspaceUiState,
    recent: List<RecentPackage>,
    themeChoice: ThemeChoice,
    onIntent: (LogPlayerIntent) -> Unit,
    onScreen: (AppScreen) -> Unit,
    onThemeChoice: (ThemeChoice) -> Unit,
    onAbout: () -> Unit,
) {
    MenuBar {
        SessionMenu(
            workspace = workspace,
            recent = recent,
            onIntent = onIntent,
            onOpenRecent = { path ->
                onIntent(LogPlayerIntent.OpenSession(path = path))
                onScreen(AppScreen.PLAYER)
            },
            onNewSession = {
                onIntent(LogPlayerIntent.StartNewSession)
                onScreen(AppScreen.PLAYER)
            },
            onShowSessions = { onScreen(AppScreen.SESSIONS) },
        )

        ViewMenu(choice = themeChoice, onChoice = onThemeChoice)

        // Only rendered where there is no application menu to hang "About" on, which is Windows and
        // Linux, and there "Help > About" is exactly where it is looked for. macOS never reaches
        // this branch, so its habit of grafting a search field onto any menu named "Help" never
        // applies: there the same window opens from the application menu instead.
        if (!hasApplicationAboutMenu) {
            Menu(text = stringResource(resource = Res.string.legal_menu)) {
                Item(text = stringResource(resource = Res.string.legal_menu_item), onClick = onAbout)
            }
        }
    }
}

/**
 * Takes documents the desktop environment hands to the running application.
 *
 * A file opened from the Finder replaces what is on screen, exactly as one named on the command line
 * does — the same statement about what should be open, arriving by a different road. The handler
 * itself is installed before the first window; this only says where the paths go once there is one.
 */
@Composable
private fun RegisterDocumentOpenHandler(
    component: DesktopAppComponent,
    onOpened: () -> Unit,
) {
    DisposableEffect(key1 = component) {
        DocumentOpenRequests.onOpen { paths ->
            // The Apple Event arrives on the AWT thread; the screen it changes is Compose state.
            component.applicationScope.launch {
                openStartupFiles(paths = paths, store = component.store)
                onOpened()
            }
        }
        onDispose { DocumentOpenRequests.stopListening() }
    }
}

/**
 * Hands "About" to the platform where the platform owns it.
 *
 * Registered for the lifetime of the application rather than of a window, because the entry sits in
 * the application menu and stays there whether or not a window is up.
 */
@Composable
private fun RegisterAboutHandler(onAbout: () -> Unit) {
    DisposableEffect(key1 = Unit) {
        if (hasApplicationAboutMenu) {
            Desktop.getDesktop().setAboutHandler { onAbout() }
        }
        onDispose { if (hasApplicationAboutMenu) Desktop.getDesktop().setAboutHandler(null) }
    }
}

/**
 * Routes command line arguments by file type.
 *
 * A session file supersedes the workspace that was restored: naming one on the command line is a
 * clear statement about what should be open, and it is the only argument that can carry a whole
 * workspace rather than a single file.
 *
 * Unsupported paths are still forwarded as logs on purpose: the import rejects them with a message,
 * which is more helpful than silently ignoring a file the user explicitly asked for.
 */
internal fun openStartupFiles(paths: List<String>, store: LogPlayerStore) {
    val session = paths.firstOrNull { SessionFile.matches(path = it) }
    if (session != null) {
        store.handleIntent(intent = LogPlayerIntent.OpenSession(path = session))
        return
    }
    val videos = paths.filter { SupportedFileTypes.accepts(kind = MediaKind.VIDEO, path = it) }
    videos.firstOrNull()?.let { path -> store.handleIntent(intent = LogPlayerIntent.ImportVideo(path = path)) }
    store.handleIntent(intent = LogPlayerIntent.ImportLogFiles(paths = paths - videos.toSet()))
}

/** `MJLogs — investigation •`, where the marker means a full package is behind what is on screen. */
internal fun windowTitle(base: String, sessionName: String?, hasUnsavedChanges: Boolean): String = when {
    sessionName == null -> base
    hasUnsavedChanges -> "$base — $sessionName •"
    else -> "$base — $sessionName"
}
