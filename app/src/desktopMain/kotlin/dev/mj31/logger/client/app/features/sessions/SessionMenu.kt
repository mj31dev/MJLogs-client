package dev.mj31.logger.client.app.features.sessions

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBarScope
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ui.WorkspaceUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.session_menu
import dev.mj31.logger.client.app.resources.session_menu_all
import dev.mj31.logger.client.app.resources.session_menu_new
import dev.mj31.logger.client.app.resources.session_menu_open
import dev.mj31.logger.client.app.resources.session_menu_recent
import dev.mj31.logger.client.app.resources.session_menu_save
import dev.mj31.logger.client.app.resources.session_menu_save_as
import dev.mj31.logger.client.app.resources.sessions_menu_recent_empty
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import org.jetbrains.compose.resources.stringResource

/** How many saved sessions the menu lists before the start screen is the better place to look. */
private const val MENU_RECENT_LIMIT = 10

/**
 * Everything a workspace can do as a file.
 *
 * The recent sessions are listed in place rather than behind an entry that opens the start screen:
 * reopening one of the last few is the common case, and a menu is exactly the right shape for a
 * short list of files. The screen stays for the cases the menu is bad at — a long list, and removing
 * an entry from it.
 *
 * "Save session" only appears once the workspace belongs to a file: before that there is nothing to
 * save into, and the two "save as" entries are the ones that create it.
 */
@Composable
fun MenuBarScope.SessionMenu(
    workspace: WorkspaceUiState,
    recent: List<RecentPackage>,
    onIntent: (LogPlayerIntent) -> Unit,
    onOpenRecent: (String) -> Unit,
    onNewSession: () -> Unit,
    onShowSessions: () -> Unit,
) {
    Menu(text = stringResource(resource = Res.string.session_menu)) {
        Item(text = stringResource(resource = Res.string.session_menu_new), onClick = onNewSession)
        Item(
            text = stringResource(resource = Res.string.session_menu_open),
            onClick = { onIntent(LogPlayerIntent.RequestOpenSession) },
        )
        // The way back to the start screen, which the menu list deliberately does not replace: it
        // is where a long list is browsed and the only place an entry can be removed.
        Item(text = stringResource(resource = Res.string.session_menu_all), onClick = onShowSessions)
        Separator()
        Menu(text = stringResource(resource = Res.string.session_menu_recent)) {
            if (recent.isEmpty()) {
                // A submenu that opens onto nothing looks broken; saying so costs one disabled row.
                Item(
                    text = stringResource(resource = Res.string.sessions_menu_recent_empty),
                    enabled = false,
                    onClick = {},
                )
            }
            recent.take(n = MENU_RECENT_LIMIT).forEach { entry ->
                Item(text = entry.name, onClick = { onOpenRecent(entry.path) })
            }
        }
        Separator()
        if (workspace.isBoundToPackage) {
            Item(
                text = stringResource(resource = Res.string.session_menu_save),
                enabled = workspace.hasUnsavedChanges && !workspace.isSaving,
                onClick = { onIntent(LogPlayerIntent.SaveSessionChanges) },
            )
        }
        Item(
            text = stringResource(resource = Res.string.session_menu_save_as),
            enabled = !workspace.isSaving,
            onClick = { onIntent(LogPlayerIntent.RequestSaveSession) },
        )
    }
}
