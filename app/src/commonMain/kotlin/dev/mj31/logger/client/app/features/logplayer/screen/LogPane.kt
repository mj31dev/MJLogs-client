package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.LogRow
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.log_add_files
import dev.mj31.logger.client.app.resources.log_empty_description
import dev.mj31.logger.client.app.resources.log_jump_to_playhead
import dev.mj31.logger.client.app.resources.log_empty_title
import dev.mj31.logger.client.app.resources.log_no_match_description
import dev.mj31.logger.client.app.resources.log_no_match_title
import dev.mj31.logger.client.app.resources.log_record_count
import dev.mj31.logger.client.app.resources.log_record_count_filtered
import dev.mj31.logger.client.app.resources.log_reset_filters
import dev.mj31.logger.client.app.resources.log_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Right half of the workspace: the merged log session with its filters.
 *
 * The pane is a pure function of [LogPlayerState]; every interaction is reported upwards so the
 * view model stays the single owner of the state.
 */
@Composable
fun LogPane(
    state: LogPlayerState,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onFilterChange: (LogFilter) -> Unit = { filter -> onIntent(LogPlayerIntent.UpdateFilter(filter = filter)) }
    val onImportLogsClick = { onIntent(LogPlayerIntent.RequestLogImport) }

    // Hoisted so the header can drive it. Where the list is scrolled to is not part of the workspace
    // and never outlives the screen, so it stays here rather than becoming a field of the state.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.background)
            .padding(all = 12.dp),
    ) {
        LogPaneHeader(
            state = state,
            onJumpToPlayhead = {
                val index = state.entries.indexOfFirst { it.id == state.activeEntryId }
                if (index >= 0) scope.launch { listState.animateScrollToItem(index = index) }
            },
            onImportLogsClick = onImportLogsClick,
        )

        if (state.sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(height = 8.dp))
            SourceChips(state = state, onFilterChange = onFilterChange)
        }

        Spacer(modifier = Modifier.height(height = 8.dp))

        FilterBar(state = state, onIntent = onIntent)

        Spacer(modifier = Modifier.height(height = 8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !state.hasLogs -> EmptyState(
                    title = stringResource(resource = Res.string.log_empty_title),
                    description = stringResource(resource = Res.string.log_empty_description),
                    actionLabel = stringResource(resource = Res.string.log_add_files),
                    onAction = onImportLogsClick,
                )

                state.entries.isEmpty() -> EmptyState(
                    title = stringResource(resource = Res.string.log_no_match_title),
                    description = stringResource(
                        resource = Res.string.log_no_match_description,
                        state.totalEntryCount,
                    ),
                    actionLabel = stringResource(resource = Res.string.log_reset_filters),
                    onAction = { onFilterChange(LogFilter()) },
                )

                else -> LogList(
                    state = state,
                    listState = listState,
                    onEntrySelected = { id -> onIntent(LogPlayerIntent.SelectEntry(entryId = id)) },
                )
            }
        }
    }
}

@Composable
private fun LogPaneHeader(
    state: LogPlayerState,
    onJumpToPlayhead: () -> Unit,
    onImportLogsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = stringResource(resource = Res.string.log_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (state.isFiltered) {
                    stringResource(
                        resource = Res.string.log_record_count_filtered,
                        state.entries.size,
                        state.totalEntryCount,
                    )
                } else {
                    stringResource(resource = Res.string.log_record_count, state.totalEntryCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Always present, disabled when there is nowhere to jump: a control that came and went
            // with the synchronization would shift the button beside it every time an anchor
            // changed. Quieter than "Add logs…" — the pane already has its one filled button.
            OutlinedButton(
                onClick = onJumpToPlayhead,
                enabled = state.activeEntryId != null,
            ) {
                Text(text = stringResource(resource = Res.string.log_jump_to_playhead))
            }
            Button(onClick = onImportLogsClick) {
                Text(text = stringResource(resource = Res.string.log_add_files))
            }
        }
    }
}

@Composable
private fun LogList(
    state: LogPlayerState,
    listState: LazyListState,
    onEntrySelected: (String) -> Unit,
) {
    val entries = state.entries

    // Keep the record under the playhead visible, but never fight a user who is scrolling.
    LaunchedEffect(key1 = state.activeEntryId, key2 = state.followVideo) {
        val activeId = state.activeEntryId
        if (!state.followVideo || activeId == null || listState.isScrollInProgress) return@LaunchedEffect
        val index = entries.indexOfFirst { it.id == activeId }
        if (index >= 0) listState.animateScrollToItem(index = index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp),
        ) {
            items(items = entries, key = { it.id }) { entry ->
                LogRow(
                    entry = entry,
                    isSelected = entry.id == state.selectedEntryId,
                    isActive = entry.id == state.activeEntryId,
                    isAnchor = entry.id == state.sync.anchorEntryId,
                    onClick = { onEntrySelected(entry.id) },
                )
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
private fun EmptyState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(height = 6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(height = 14.dp))
        TextButton(onClick = onAction) { Text(text = actionLabel) }
    }
}

@Composable
private fun SourceChips(state: LogPlayerState, onFilterChange: (LogFilter) -> Unit) {
    val allIds = remember(key1 = state.sources) { state.sources.map { it.id }.toSet() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        state.sources.forEach { source ->
            SourceChip(
                source = source,
                onClick = {
                    onFilterChange(
                        state.filter.copy(
                            sourceIds = toggleSource(
                                current = state.filter.sourceIds,
                                allIds = allIds,
                                toggled = source.id,
                            ),
                        ),
                    )
                },
            )
        }
    }
}

/**
 * An empty selection means "every source is visible", so toggling has to expand the implicit set
 * first and collapse it back to empty once everything is selected again. Hiding the last remaining
 * source is a no-op: an empty session view would be indistinguishable from "show everything".
 */
internal fun toggleSource(current: Set<String>, allIds: Set<String>, toggled: String): Set<String> {
    val effective = if (current.isEmpty()) allIds else current
    val updated = if (toggled in effective) effective - toggled else effective + toggled
    return when {
        updated.isEmpty() -> current
        updated == allIds -> emptySet()
        else -> updated
    }
}
