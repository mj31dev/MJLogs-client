package dev.mj31.logger.client.app.features.sessions

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBarScope
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.view_menu
import dev.mj31.logger.client.app.resources.view_theme_dark
import dev.mj31.logger.client.app.resources.view_theme_light
import dev.mj31.logger.client.app.resources.view_theme_system
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import org.jetbrains.compose.resources.stringResource

/**
 * How the window looks, which is the only thing this menu is for so far.
 *
 * The three entries are radio items rather than a single toggle because "match the system" is a
 * third state, not the absence of a choice: someone who has pinned the light scheme wants to see
 * that they pinned it.
 */
@Composable
fun MenuBarScope.ViewMenu(
    choice: ThemeChoice,
    onChoice: (ThemeChoice) -> Unit,
) {
    Menu(text = stringResource(resource = Res.string.view_menu)) {
        RadioButtonItem(
            text = stringResource(resource = Res.string.view_theme_system),
            selected = choice == ThemeChoice.SYSTEM,
            onClick = { onChoice(ThemeChoice.SYSTEM) },
        )
        RadioButtonItem(
            text = stringResource(resource = Res.string.view_theme_light),
            selected = choice == ThemeChoice.LIGHT,
            onClick = { onChoice(ThemeChoice.LIGHT) },
        )
        RadioButtonItem(
            text = stringResource(resource = Res.string.view_theme_dark),
            selected = choice == ThemeChoice.DARK,
            onClick = { onChoice(ThemeChoice.DARK) },
        )
    }
}
