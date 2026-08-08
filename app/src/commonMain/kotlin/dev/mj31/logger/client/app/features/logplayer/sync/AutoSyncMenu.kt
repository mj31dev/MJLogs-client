package dev.mj31.logger.client.app.features.logplayer.sync

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ui.AutoSyncUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.sync_auto_menu
import dev.mj31.logger.client.app.resources.sync_auto_pick_region
import dev.mj31.logger.client.app.resources.sync_auto_refine
import dev.mj31.logger.client.app.resources.sync_auto_run
import org.jetbrains.compose.resources.stringResource

/**
 * The ways of synchronizing that need no argument from the user, gathered behind one control.
 *
 * The two manual buttons stay where they were, in the open: they are what a user reaches for when
 * they already know the answer, and burying them a click deeper to make room for automation would be
 * the wrong trade. What goes in here is everything that has to *look* for the answer, which is also
 * everything that takes time and can fail.
 */
@Composable
fun AutoSyncMenu(
    autoSync: AutoSyncUiState,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(value = false) }

    OutlinedButton(
        onClick = { isOpen = true },
        enabled = autoSync.canRun,
        modifier = modifier,
    ) {
        Text(text = stringResource(resource = Res.string.sync_auto_menu))
    }

    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        DropdownMenuItem(
            text = { Text(text = stringResource(resource = Res.string.sync_auto_run)) },
            onClick = {
                isOpen = false
                onIntent(LogPlayerIntent.SynchronizeAutomatically)
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(resource = Res.string.sync_auto_refine)) },
            enabled = autoSync.canRefine,
            onClick = {
                isOpen = false
                onIntent(LogPlayerIntent.RefineWithScreenClock)
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(resource = Res.string.sync_auto_pick_region)) },
            onClick = {
                isOpen = false
                onIntent(LogPlayerIntent.RequestClockRegion)
            },
        )
    }
}
