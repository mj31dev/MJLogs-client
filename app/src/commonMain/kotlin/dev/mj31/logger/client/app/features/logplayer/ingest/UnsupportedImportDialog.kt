package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.UnsupportedImportUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.unsupported_import_confirm
import dev.mj31.logger.client.app.resources.unsupported_import_question
import dev.mj31.logger.client.app.resources.unsupported_import_skip
import dev.mj31.logger.client.app.resources.unsupported_import_title
import dev.mj31.logger.client.app.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Asks whether a file the workspace does not recognize by name should be read as a log regardless.
 *
 * One import can be refused several files at a time, and the state hands over one at a time, so this
 * dialog answers about exactly one and comes back for the next. That is why the name and the folder
 * it came from are shown as the subject of the question rather than left inside the sentence
 * underneath: saying "open it anyway" about the wrong file is the only way this dialog can fail.
 */
@Composable
fun UnsupportedImportDialog(
    request: UnsupportedImportUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onIntent(LogPlayerIntent.DismissUnsupportedImport) },
        title = { Text(text = stringResource(resource = Res.string.unsupported_import_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.medium),
            ) {
                ImportFileCard(fileName = request.fileName, path = request.path)

                // The refusal and the question it leads to are one paragraph, held closer to each
                // other than to the file they are about.
                Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
                    // The store already phrased the refusal, naming the file and the accepted
                    // extensions; it is shown as it arrived.
                    Text(
                        text = request.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = stringResource(resource = Res.string.unsupported_import_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onIntent(LogPlayerIntent.ConfirmUnsupportedImport) }) {
                Text(text = stringResource(resource = Res.string.unsupported_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(LogPlayerIntent.DismissUnsupportedImport) }) {
                Text(text = stringResource(resource = Res.string.unsupported_import_skip))
            }
        },
    )
}
