package dev.mj31.logger.client.app.features.logplayer.ingest

import dev.mj31.logger.client.app.theme.type.ContentType
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.ui.Alignment
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.SourcePreambleUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.preamble_caption
import dev.mj31.logger.client.app.resources.preamble_close
import dev.mj31.logger.client.app.resources.source_menu_preamble
import dev.mj31.logger.client.app.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Test tag of the one button that closes the file header viewer. */
const val PREAMBLE_DISMISS_TAG = "preamble-dismiss"

/** Test tag of the block holding the header's lines. */
const val PREAMBLE_TEXT_TAG = "preamble-text"

private const val MAX_BLOCK_HEIGHT = 360
private const val TITLE_LINES = 1

/**
 * The text a log file carries before its first record, open for reading.
 *
 * The lines are log content, not interface, so they get what log records get: the monospaced face,
 * the same size, and no wrapping — a banner drawn in columns is only readable if the columns survive.
 * They sit inset on `surfaceVariant` and scroll in both directions inside a bounded block, so a
 * hundred lines or one very long one never push the dialog past the window. The block is selectable
 * because the usual reason to open it is to copy a device name or a build number out of it.
 *
 * The view exists to be read, not to be answered, so there is no filled button: closing it is
 * housekeeping, and a `TextButton` says so.
 */
@Composable
fun SourcePreambleDialog(
    preamble: SourcePreambleUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onDismiss = { onIntent(LogPlayerIntent.DismissSourcePreamble) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // The file is the subject of the dialog, so its name is the title; what is being shown
            // of it follows as the label under it.
            Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline)) {
                Text(
                    text = preamble.fileName,
                    maxLines = TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(resource = Res.string.source_menu_preamble),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.medium),
            ) {
                Text(
                    text = stringResource(resource = Res.string.preamble_caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerbatimLines(lines = preamble.lines)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(tag = PREAMBLE_DISMISS_TAG),
            ) {
                Text(text = stringResource(resource = Res.string.preamble_close))
            }
        },
    )
}

/** The lines as one selectable, unwrapped block, inset and bounded, scrolling both ways. */
@Composable
private fun VerbatimLines(lines: List<String>) {
    val text = remember(key1 = lines) { lines.joinToString(separator = "\n") }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = MAX_BLOCK_HEIGHT.dp)
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .testTag(tag = PREAMBLE_TEXT_TAG),
    ) {
        SelectionContainer {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = ContentType.record,
                softWrap = false,
                modifier = Modifier
                    .verticalScroll(state = verticalScroll)
                    .horizontalScroll(state = horizontalScroll)
                    .padding(all = Spacing.medium),
            )
        }

        // Both bars, because both overflows are ordinary here: a long banner and a single line
        // wider than the dialog. Each draws only while there is somewhere to scroll.
        Box(modifier = Modifier.matchParentSize()) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState = verticalScroll),
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState = horizontalScroll),
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}
