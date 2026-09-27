package dev.mj31.logger.client.app.features.logplayer.duplicate

import dev.mj31.logger.client.app.resources.duplicate_existing_label
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.ingest.ImportFileCard
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateChoice
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateKind
import dev.mj31.logger.client.app.features.logplayer.state.duplicate.DuplicateRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.duplicate_choice_add_separately_copy
import dev.mj31.logger.client.app.resources.duplicate_choice_add_separately_overlap
import dev.mj31.logger.client.app.resources.duplicate_choice_merge
import dev.mj31.logger.client.app.resources.duplicate_choice_skip
import dev.mj31.logger.client.app.resources.duplicate_copy_explanation
import dev.mj31.logger.client.app.resources.duplicate_copy_title
import dev.mj31.logger.client.app.resources.duplicate_overlap_explanation
import dev.mj31.logger.client.app.resources.duplicate_overlap_title
import dev.mj31.logger.client.app.theme.Spacing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Test tag of the button for one [DuplicateChoice], suffixed with the choice's name. */
const val DUPLICATE_CHOICE_TAG = "duplicate-choice"

/**
 * Asks what to do with a file that repeats one already open.
 *
 * The first of the offered choices is the one the dialog recommends, and it alone is filled: for a
 * copy that is leaving the file out, since opening it only doubles every record; for an overlap it
 * is merging, which is what someone who caught the same log twice nearly always wants. Opening the
 * file on its own is a real alternative and carries an outline; skipping, where it is not the
 * recommendation, is housekeeping and carries none.
 *
 * The new file is the subject and is shown as the other import dialogs show theirs, name over folder,
 * because two files of one name are exactly what this question tends to be about. The open file is
 * named in the prose, by the name its chip carries.
 *
 * Dismissing the dialog answers it: closing a question about a repeat means "not now", which is Skip.
 */
@Composable
fun DuplicateDialog(
    request: DuplicateRequestUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val resolve = { choice: DuplicateChoice -> onIntent(LogPlayerIntent.ResolveDuplicate(choice = choice)) }
    val recommended = request.choices.first()
    val alternatives = request.choices.drop(n = 1)

    AlertDialog(
        onDismissRequest = { resolve(DuplicateChoice.SKIP) },
        title = { Text(text = stringResource(resource = titleOf(kind = request.kind))) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.medium),
            ) {
                ImportFileCard(fileName = request.candidate.name, path = request.candidate.path)

                Text(
                    text = explanationOf(request = request),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(resource = Res.string.duplicate_existing_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ImportFileCard(fileName = request.existingName, path = request.existingPath)
            }
        },
        confirmButton = {
            Button(
                onClick = { resolve(recommended) },
                modifier = Modifier.testTag(tag = choiceTag(choice = recommended)),
            ) {
                Text(text = labelOf(choice = recommended, kind = request.kind))
            }
        },
        dismissButton = {
            // Read from the edge inwards: the quietest choice farthest from the filled one.
            Row(horizontalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
                alternatives.asReversed().forEach { choice ->
                    AlternativeButton(
                        choice = choice,
                        label = labelOf(choice = choice, kind = request.kind),
                        onClick = { resolve(choice) },
                    )
                }
            }
        },
    )
}

/** The tag a choice's button carries, e.g. `duplicate-choice-MERGE`. */
fun choiceTag(choice: DuplicateChoice): String = "$DUPLICATE_CHOICE_TAG-${choice.name}"

@Composable
private fun AlternativeButton(
    choice: DuplicateChoice,
    label: String,
    onClick: () -> Unit,
) {
    val modifier = Modifier.testTag(tag = choiceTag(choice = choice))
    if (choice == DuplicateChoice.SKIP) {
        TextButton(onClick = onClick, modifier = modifier) { Text(text = label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text = label) }
    }
}

private fun titleOf(kind: DuplicateKind) = when (kind) {
    DuplicateKind.COPY -> Res.string.duplicate_copy_title
    DuplicateKind.OVERLAP -> Res.string.duplicate_overlap_title
}

@Composable
private fun explanationOf(request: DuplicateRequestUiState): String = when (request.kind) {
    DuplicateKind.COPY -> stringResource(resource = Res.string.duplicate_copy_explanation, request.existingName)
    DuplicateKind.OVERLAP -> pluralStringResource(
        resource = Res.plurals.duplicate_overlap_explanation,
        quantity = request.sharedRecordCount,
        request.sharedRecordCount,
        request.candidate.entryCount,
        request.existingName,
    )
}

/** "Open anyway" beside a copy, where opening it is the odd choice; "Open separately" beside a merge. */
@Composable
private fun labelOf(choice: DuplicateChoice, kind: DuplicateKind): String = stringResource(
    resource = when (choice) {
        DuplicateChoice.MERGE -> Res.string.duplicate_choice_merge
        DuplicateChoice.SKIP -> Res.string.duplicate_choice_skip
        DuplicateChoice.ADD_SEPARATELY -> when (kind) {
            DuplicateKind.COPY -> Res.string.duplicate_choice_add_separately_copy
            DuplicateKind.OVERLAP -> Res.string.duplicate_choice_add_separately_overlap
        }
    },
)
