package dev.mj31.logger.client.app.features.logplayer.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.state.ui.PackageSaveUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.session_save_cancel
import dev.mj31.logger.client.app.resources.session_save_progress_file
import dev.mj31.logger.client.app.resources.session_save_progress_title
import org.jetbrains.compose.resources.stringResource

/**
 * What a long save looks like while it runs.
 *
 * A full package copies a screencast, which takes long enough that a frozen window would look like a
 * hung application. The way out is next to the progress, because a copy that cannot be stopped is
 * the reason people force-quit.
 */
@Composable
fun SessionSaveBar(
    save: PackageSaveUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = SAVE_BAR_TAG },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(all = 12.dp), verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(resource = Res.string.session_save_progress_title),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            resource = Res.string.session_save_progress_file,
                            save.fileName,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(resource = Res.string.session_save_cancel))
                }
            }

            // A package that bundles nothing has no bytes to count, and a bar stuck at zero would
            // claim it is not moving when in fact it is nearly done.
            if (save.totalBytes > 0L) {
                LinearProgressIndicator(progress = { save.fraction }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Handle the UI tests find this bar by. */
const val SAVE_BAR_TAG: String = "session-save-bar"
