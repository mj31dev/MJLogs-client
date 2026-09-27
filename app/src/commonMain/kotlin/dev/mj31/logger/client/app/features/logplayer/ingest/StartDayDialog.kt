package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.StartDayRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.start_day_confirm
import dev.mj31.logger.client.app.resources.start_day_explanation
import dev.mj31.logger.client.app.resources.start_day_question
import dev.mj31.logger.client.app.resources.start_day_skip
import dev.mj31.logger.client.app.resources.start_day_title
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.format.formatCalendarDate
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val BORDER_WIDTH = 1

/**
 * Asks which of two days a log that records the time but not the date begins on.
 *
 * The dialog exists only for the case where the evidence runs out: the times inside the file run
 * past midnight, so it covers two days, and the moment the file was last written rules out neither
 * pair. Where that moment does rule one out the application takes the other and asks nothing, so
 * anything reaching here is a genuine coin toss — which is why the two days are drawn identically,
 * neither pre-selected and neither given the weight of a button of its own. Offering one as the
 * default would claim knowledge the application does not have, and the cost of it being wrong is
 * every record of the file sitting a day away from the screencast.
 *
 * The single filled button therefore confirms a choice rather than being a choice. It is the one
 * shape in which "pick one of two equals" and "one filled button, and it is what the view exists
 * for" are both true; the extra click is the price of not lying, and this question is rare.
 */
@Composable
fun StartDayDialog(
    request: StartDayRequestUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onDismiss = { onIntent(LogPlayerIntent.DismissStartDayRequest) }
    // View state, and only that: which row is ticked. It is dropped when the queue moves on to the
    // next file, so an answer can never carry over to a question it was not given for.
    var chosen by remember(key1 = request.path) { mutableStateOf<LocalDate?>(value = null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.start_day_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.large),
            ) {
                ImportFileCard(fileName = request.fileName, path = request.path)

                // Why the question is being asked. Without it the question looks arbitrary — a
                // dialog demanding a date for reasons of its own.
                Text(
                    text = stringResource(resource = Res.string.start_day_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // The question labels the two rows rather than continuing the prose, so it travels
                // with them.
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(space = Spacing.small),
                ) {
                    Text(
                        text = stringResource(resource = Res.string.start_day_question),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    request.candidates.forEach { candidate ->
                        DayOption(
                            day = candidate,
                            isChosen = candidate == chosen,
                            onChoose = { chosen = candidate },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { chosen?.let { day -> onIntent(LogPlayerIntent.ChooseStartDay(day = day)) } },
                enabled = chosen != null,
            ) {
                Text(text = stringResource(resource = Res.string.start_day_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.start_day_skip))
            }
        },
    )
}

/**
 * One of the two days, as a row the whole width of the dialog.
 *
 * Both rows are the same shape and the same weight; only the tick moves. The written-out date leads
 * because a bare `2026-08-12` is hard to place in a memory of a recording, and the ISO form follows
 * as the metadata under it, since that is the form the records themselves will carry once the file
 * is read.
 */
@Composable
private fun DayOption(
    day: LocalDate,
    isChosen: Boolean,
    onChoose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.small)
            .background(
                color = if (isChosen) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            // The unchosen row sits on the same level as the dialog under it, which is the one place
            // a border belongs; chosen, it keeps one so that the row does not change size.
            .border(
                width = BORDER_WIDTH.dp,
                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small,
            )
            .selectable(selected = isChosen, role = Role.RadioButton, onClick = onChoose)
            .padding(all = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(space = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row carries the selection semantics, so the button is decoration with a shape.
        RadioButton(selected = isChosen, onClick = null)

        Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline)) {
            Text(
                text = formatCalendarDate(date = day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
