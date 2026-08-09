package dev.mj31.logger.client.app.features.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.sessions_continue
import dev.mj31.logger.client.app.resources.sessions_empty
import dev.mj31.logger.client.app.resources.sessions_forget
import dev.mj31.logger.client.app.resources.sessions_last_summary
import dev.mj31.logger.client.app.resources.sessions_last_with_video
import dev.mj31.logger.client.app.resources.sessions_new
import dev.mj31.logger.client.app.resources.sessions_open_file
import dev.mj31.logger.client.app.resources.sessions_recent
import dev.mj31.logger.client.app.resources.sessions_title
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.format.formatWallClock
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** One radius for everything here, so the column reads as one family rather than three widgets. */
private val CardShape = RoundedCornerShape(size = 10.dp)

/** A launcher centred in a window sized for two panes would otherwise stretch across 1440dp. */
private val ContentMaxWidth = 680.dp

/**
 * Where a launch lands: carry on, reopen something, or start fresh.
 *
 * A launcher rather than a manager. Sessions come in handfuls, the screen is visited once per run,
 * and its whole job is to be left quickly — so it is the one surface in the application that can
 * afford to be comfortable, and the only one that is. The log pane never has that luxury.
 *
 * The three ways out are deliberately not equal. Continuing is what someone who just closed the
 * application is most likely to want, so it is the widest thing here and needs no reading; starting
 * fresh is the single filled button; the saved files are a quiet list underneath.
 */
@Composable
fun SessionsScreen(
    state: SessionsState,
    onIntent: (SessionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The screen paints its own background. Inheriting it left this one drawing dark cards on the
    // default white while the rest of the application was near-black.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.section, vertical = Spacing.xlarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = Spacing.large),
        ) {
            Text(
                text = stringResource(resource = Res.string.sessions_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            state.lastSession?.let { last -> ContinueCard(last = last, onIntent = onIntent) }

            Row(horizontalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
                Button(onClick = { onIntent(SessionsIntent.StartNew) }) {
                    Text(text = stringResource(resource = Res.string.sessions_new))
                }
                OutlinedButton(onClick = { onIntent(SessionsIntent.RequestOpenFile) }) {
                    Text(text = stringResource(resource = Res.string.sessions_open_file))
                }
            }

            RecentSection(recent = state.recent, onIntent = onIntent)
        }
    }
}

/**
 * The widest thing on the screen, because it is the likeliest.
 *
 * It stays on `surface` like the rows below rather than inventing a fourth level: the weight comes
 * from its size, its padding and the one coloured line above the name.
 */
@Composable
private fun ContinueCard(
    last: LastSessionUi,
    onIntent: (SessionsIntent) -> Unit,
) {
    Surface(
        onClick = { onIntent(SessionsIntent.ContinueLast) },
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(all = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline),
        ) {
            Text(
                text = stringResource(resource = Res.string.sessions_continue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = last.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summaryOf(last = last),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun summaryOf(last: LastSessionUi): String {
    val logs = pluralStringResource(
        resource = Res.plurals.sessions_last_summary,
        quantity = last.logCount,
        last.logCount,
    )
    return if (last.hasVideo) {
        "$logs · ${stringResource(resource = Res.string.sessions_last_with_video)}"
    } else {
        logs
    }
}

@Composable
private fun RecentSection(
    recent: List<RecentPackage>,
    onIntent: (SessionsIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
        Text(
            text = stringResource(resource = Res.string.sessions_recent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.small),
        )

        if (recent.isEmpty()) {
            Text(
                text = stringResource(resource = Res.string.sessions_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
            items(items = recent, key = { it.path }) { entry ->
                RecentSessionRow(entry = entry, onIntent = onIntent)
            }
        }
    }
}

/**
 * One saved file.
 *
 * The row is the button: it names one file and carries one verb, so a control repeating that verb
 * would only take space. Removing is a second verb and a quiet one — it tidies the list, and it must
 * never be the loudest thing on a card that names something.
 */
@Composable
private fun RecentSessionRow(
    entry: RecentPackage,
    onIntent: (SessionsIntent) -> Unit,
) {
    Surface(
        onClick = { onIntent(SessionsIntent.Open(path = entry.path)) },
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = Spacing.large,
                end = Spacing.small,
                top = Spacing.medium,
                bottom = Spacing.medium,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline),
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatWallClock(instant = entry.lastOpened)} · ${entry.path}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onIntent(SessionsIntent.Forget(path = entry.path)) }) {
                Text(
                    text = stringResource(resource = Res.string.sessions_forget),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
