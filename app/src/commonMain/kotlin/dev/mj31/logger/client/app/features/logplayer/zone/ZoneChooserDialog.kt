package dev.mj31.logger.client.app.features.logplayer.zone

import dev.mj31.logger.client.app.view.modifier.fill
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.zone_automatic_screen
import dev.mj31.logger.client.app.resources.zone_automatic_source
import dev.mj31.logger.client.app.resources.zone_cancel
import dev.mj31.logger.client.app.resources.zone_current
import dev.mj31.logger.client.app.resources.zone_current_marker
import dev.mj31.logger.client.app.resources.zone_dialog_label
import dev.mj31.logger.client.app.resources.zone_dialog_screen_clock
import dev.mj31.logger.client.app.resources.zone_no_match
import dev.mj31.logger.client.app.resources.zone_origin_chosen
import dev.mj31.logger.client.app.resources.zone_origin_default
import dev.mj31.logger.client.app.resources.zone_origin_follows_logs
import dev.mj31.logger.client.app.resources.zone_origin_header
import dev.mj31.logger.client.app.resources.zone_origin_line
import dev.mj31.logger.client.app.resources.zone_search_label
import dev.mj31.logger.client.app.resources.zone_search_placeholder
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import org.jetbrains.compose.resources.stringResource

/** Test tag of the field that filters the zones and accepts a typed offset. */
const val ZONE_SEARCH_TAG = "zone-search"

/** Test tag of one zone in the list, suffixed with the zone id — `zone-option-Europe/Berlin`. */
const val ZONE_OPTION_TAG = "zone-option"

/** Test tag of the row that hands the decision back: the file's own zone, or the logs' zone. */
const val ZONE_AUTOMATIC_TAG = "zone-automatic"

/** Test tag of the button that closes the chooser without choosing. */
const val ZONE_DISMISS_TAG = "zone-dismiss"

private const val LIST_HEIGHT = 280
private const val TITLE_LINES = 1

/**
 * Which time zone a file's clock — or the clock on the screen — runs in.
 *
 * Picking is the whole answer, so a row applies itself on click and there is no filled button: the
 * only button left closes the dialog without a change, and that is housekeeping. The row that is in
 * force now is set on the inset surface *and* says so in words, so it does not rest on colour.
 *
 * The first row hands the decision back rather than naming a zone, because "UTC, since nothing said
 * otherwise" and "UTC, as chosen" behave differently when the file is read again. It is worded for
 * what it is handing back to: a file falls back on its header, the screen clock on the logs.
 *
 * The search field filters by any part of a region's name and also accepts an offset however it is
 * spelled (`utc+3`, `GMT-05:30`); a readable one appears at the top of the list in the form it will
 * be stored in, so what is typed and what is kept never differ silently. Enter takes the first row.
 */
@Composable
fun ZoneChooserDialog(
    request: ZoneRequestUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onDismiss = { onIntent(LogPlayerIntent.DismissZoneRequest) }
    val onChoose: (String?) -> Unit = { zoneId -> onIntent(LogPlayerIntent.ChooseZone(zoneId = zoneId)) }
    var query by remember(key1 = request.target) { mutableStateOf(value = "") }
    val candidates = remember(key1 = query, key2 = request.availableZoneIds) {
        zoneCandidates(query = query, available = request.availableZoneIds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Title(request = request) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = Spacing.medium),
            ) {
                Text(
                    text = stringResource(
                        resource = Res.string.zone_current,
                        request.current.id,
                        stringResource(resource = originDescription(request = request)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { typed -> query = typed },
                    label = { Text(text = stringResource(resource = Res.string.zone_search_label)) },
                    placeholder = { Text(text = stringResource(resource = Res.string.zone_search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { candidates.firstOrNull()?.let(onChoose) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = ZONE_SEARCH_TAG),
                )
                ZoneList(
                    request = request,
                    query = query,
                    candidates = candidates,
                    onChoose = onChoose,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(tag = ZONE_DISMISS_TAG),
            ) {
                Text(text = stringResource(resource = Res.string.zone_cancel))
            }
        },
    )
}

/** What the zone is for is the subject, so it is the title; what is being chosen follows under it. */
@Composable
private fun Title(request: ZoneRequestUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline)) {
        Text(
            text = request.subject ?: stringResource(resource = Res.string.zone_dialog_screen_clock),
            maxLines = TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(resource = Res.string.zone_dialog_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The automatic row, then the zones that match. Bounded and outlined: the list is long, and the
 * outline is where the scrolling region meets the dialog's own surface.
 */
@Composable
private fun ZoneList(
    request: ZoneRequestUiState,
    query: String,
    candidates: List<String>,
    onChoose: (String?) -> Unit,
) {
    val chosenId = request.current.id.takeIf { request.current.origin == ZoneOrigin.CHOSEN }
    val isSearching = query.isNotBlank()
    // Opens on the zone in force, so a second visit starts where the first one ended. The automatic
    // row comes first, which is what the one added to the index is.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = chosenId?.let { id -> candidates.indexOf(element = id) + 1 } ?: 0,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = LIST_HEIGHT.dp)
            .clip(shape = MaterialTheme.shapes.small)
            .border(
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.small,
            ),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (!isSearching) {
                item(key = ZONE_AUTOMATIC_TAG) {
                    ZoneRow(
                        text = stringResource(resource = automaticLabel(target = request.target)),
                        isCurrent = chosenId == null,
                        onClick = { onChoose(null) },
                        modifier = Modifier.testTag(tag = ZONE_AUTOMATIC_TAG),
                    )
                }
            }
            items(items = candidates, key = { id -> id }) { id ->
                ZoneRow(
                    text = id,
                    isCurrent = id == chosenId,
                    onClick = { onChoose(id) },
                    modifier = Modifier.testTag(tag = "$ZONE_OPTION_TAG-$id"),
                )
            }
            if (isSearching && candidates.isEmpty()) {
                item {
                    Text(
                        text = stringResource(resource = Res.string.zone_no_match, query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(all = Spacing.medium),
                    )
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState = listState),
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ZoneRow(
    text: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fill(color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else null)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = Spacing.small),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f),
        )
        if (isCurrent) {
            Text(
                text = stringResource(resource = Res.string.zone_current_marker),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun automaticLabel(target: ZoneTarget) = when (target) {
    is ZoneTarget.Source -> Res.string.zone_automatic_source
    ZoneTarget.FrameTime -> Res.string.zone_automatic_screen
}

/**
 * Where the zone in force came from. The screen clock is either chosen or borrowed from the logs;
 * how the logs came by theirs belongs to their own chooser.
 */
private fun originDescription(request: ZoneRequestUiState) = when {
    request.current.origin == ZoneOrigin.CHOSEN -> Res.string.zone_origin_chosen
    request.target == ZoneTarget.FrameTime -> Res.string.zone_origin_follows_logs
    request.current.origin == ZoneOrigin.LINE -> Res.string.zone_origin_line
    request.current.origin == ZoneOrigin.HEADER -> Res.string.zone_origin_header
    else -> Res.string.zone_origin_default
}

/**
 * The regions whose name contains [query] — a space standing for the underscore IANA names use — led
 * by the zone the query spells when it spells one the list does not already show, such as an offset.
 */
internal fun zoneCandidates(query: String, available: List<String>): List<String> {
    val needle = query.trim().replace(oldChar = ' ', newChar = '_')
    if (needle.isEmpty()) return available
    val matches = available.filter { id -> id.contains(other = needle, ignoreCase = true) }
    val spelled = SourceZone.idOf(text = query)
    return if (spelled == null || spelled in matches) matches else listOf(spelled) + (matches - spelled)
}
