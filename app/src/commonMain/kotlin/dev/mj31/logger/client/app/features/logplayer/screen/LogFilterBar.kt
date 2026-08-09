package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mj31.logger.client.app.theme.LocalLogLevelColors
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.app.features.logplayer.state.ui.LogSourceUi
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.filter_around_playhead
import dev.mj31.logger.client.app.resources.filter_query_clear
import dev.mj31.logger.client.app.resources.filter_query_placeholder
import dev.mj31.logger.client.app.resources.filter_window_15s
import dev.mj31.logger.client.app.resources.filter_window_5s
import dev.mj31.logger.client.app.resources.filter_window_60s
import dev.mj31.logger.client.app.resources.filter_window_all
import dev.mj31.logger.client.app.resources.source_chip_skipped
import dev.mj31.logger.client.app.resources.source_chip_subtitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val CHIP_FONT_SIZE = 11
private const val UNSELECTED_ALPHA = 0.35f
private const val DISABLED_ALPHA = 0.4f

private val timeWindows: List<Pair<StringResource, Long?>> = listOf(
    Res.string.filter_window_all to null,
    Res.string.filter_window_5s to 5_000L,
    Res.string.filter_window_15s to 15_000L,
    Res.string.filter_window_60s to 60_000L,
)

/** Free text, level, and time-window filters applied to the merged session. */
@Composable
fun FilterBar(
    state: LogPlayerState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onFilterChange: (LogFilter) -> Unit = { filter -> onIntent(LogPlayerIntent.UpdateFilter(filter = filter)) }
    // The field owns the typed text: keystrokes stay responsive while a large session is re-filtered,
    // and the field keeps the undo history it would lose on every external reset.
    val query = rememberTextDraft(external = state.filter.query)

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query.value,
            onValueChange = { typed ->
                query.value = typed
                onFilterChange(state.filter.copy(query = typed))
            },
            placeholder = { Text(text = stringResource(resource = Res.string.filter_query_placeholder)) },
            singleLine = true,
            trailingIcon = {
                if (query.value.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            query.value = ""
                            onFilterChange(state.filter.copy(query = ""))
                        },
                    ) {
                        Text(text = stringResource(resource = Res.string.filter_query_clear))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state = rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            LogLevel.entries.forEach { level ->
                LevelChip(
                    level = level,
                    isSelected = state.filter.levels.isEmpty() || level in state.filter.levels,
                    onClick = {
                        onFilterChange(
                            state.filter.copy(levels = toggleLevel(current = state.filter.levels, toggled = level)),
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.width(width = 12.dp))

            TimeWindowSelector(
                selectedWindow = state.timeWindowMillis,
                isEnabled = state.sync.isSynced,
                onTimeWindowChange = { window -> onIntent(LogPlayerIntent.SetTimeWindow(windowMillis = window)) },
            )
        }
    }
}

/** An empty level set means "every level"; collapsing back to the full set restores that shorthand. */
internal fun toggleLevel(current: Set<LogLevel>, toggled: LogLevel): Set<LogLevel> {
    val all = LogLevel.entries.toSet()
    val effective = current.ifEmpty { all }
    val updated = if (toggled in effective) effective - toggled else effective + toggled
    return when {
        updated.isEmpty() -> current
        updated == all -> emptySet()
        else -> updated
    }
}

@Composable
private fun LevelChip(level: LogLevel, isSelected: Boolean, onClick: () -> Unit) {
    val color = LocalLogLevelColors.current.of(level = level)
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = if (isSelected) color.copy(alpha = UNSELECTED_ALPHA) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(size = 4.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = level.name.take(n = 1),
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = CHIP_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun TimeWindowSelector(
    selectedWindow: Long?,
    isEnabled: Boolean,
    onTimeWindowChange: (Long?) -> Unit,
) {
    Text(
        text = stringResource(resource = Res.string.filter_around_playhead),
        style = MaterialTheme.typography.bodySmall,
        color = if (isEnabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
        },
    )

    Spacer(modifier = Modifier.width(width = 6.dp))

    timeWindows.forEach { (labelResource, window) ->
        val isSelected = isEnabled && selectedWindow == window
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 4.dp))
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = UNSELECTED_ALPHA)
                    } else {
                        Color.Transparent
                    },
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(size = 4.dp),
                )
                .clickable(enabled = isEnabled) { onTimeWindowChange(window) }
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = stringResource(resource = labelResource),
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
                },
                fontSize = CHIP_FONT_SIZE.sp,
            )
        }
        Spacer(modifier = Modifier.width(width = 4.dp))
    }
}

/** One imported file: click to show or hide its records inside the merged session. */
@Composable
fun SourceChip(source: LogSourceUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 6.dp))
            .background(
                color = if (source.isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(size = 6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column {
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (source.isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
            Text(
                text = sourceSubtitle(source = source),
                fontSize = CHIP_FONT_SIZE.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun sourceSubtitle(source: LogSourceUi): String {
    val skipped = if (source.skippedLineCount > 0) {
        stringResource(resource = Res.string.source_chip_skipped, source.skippedLineCount)
    } else {
        ""
    }
    return stringResource(
        resource = Res.string.source_chip_subtitle,
        source.entryCount,
        source.formatName,
    ) + skipped
}
