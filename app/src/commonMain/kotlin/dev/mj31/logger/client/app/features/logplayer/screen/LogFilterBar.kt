package dev.mj31.logger.client.app.features.logplayer.screen

import dev.mj31.logger.client.app.view.menu.AppDropdownMenu
import dev.mj31.logger.client.app.view.modifier.fill
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.resources.source_chip_menu_description
import dev.mj31.logger.client.app.resources.source_chip_menu_glyph
import dev.mj31.logger.client.app.resources.source_chip_with_preamble
import dev.mj31.logger.client.app.resources.source_menu_preamble
import dev.mj31.logger.client.app.resources.source_menu_zone
import dev.mj31.logger.client.app.resources.source_zone_assumed
import dev.mj31.logger.client.app.resources.source_zone_header
import dev.mj31.logger.client.app.resources.source_zone_line
import dev.mj31.logger.client.domain.model.time.ZoneOrigin
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import dev.mj31.logger.client.app.resources.source_chip_with_zone
import dev.mj31.logger.client.app.resources.source_chip_file_count
import dev.mj31.logger.client.app.resources.source_chip_with_files
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val UNSELECTED_ALPHA = 0.35f
private const val DISABLED_ALPHA = 0.4f
private const val BORDER_WIDTH = 1

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

        Spacer(modifier = Modifier.height(height = Spacing.small))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state = rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = Spacing.tight),
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

            Spacer(modifier = Modifier.width(width = Spacing.medium))

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
            .clip(shape = MaterialTheme.shapes.extraSmall)
            .fill(color = if (isSelected) color.copy(alpha = UNSELECTED_ALPHA) else null)
            .border(
                width = BORDER_WIDTH.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.small, vertical = Spacing.hairline),
    ) {
        Text(
            text = level.name.take(n = 1),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
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

    Spacer(modifier = Modifier.width(width = Spacing.small))

    timeWindows.forEach { (labelResource, window) ->
        val isSelected = isEnabled && selectedWindow == window
        Box(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .fill(color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = UNSELECTED_ALPHA) else null)
                .border(
                    width = BORDER_WIDTH.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .clickable(enabled = isEnabled) { onTimeWindowChange(window) }
                .padding(horizontal = Spacing.small, vertical = Spacing.hairline),
        ) {
            Text(
                text = stringResource(resource = labelResource),
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(modifier = Modifier.width(width = Spacing.tight))
    }
}

/** Test tag of the control that opens a source chip's menu, suffixed with the source id. */
const val SOURCE_CHIP_MENU_TAG = "source-chip-menu"

/** Test tag of the menu item that opens a file's header, suffixed with the source id. */
const val SOURCE_MENU_PREAMBLE_TAG = "source-menu-preamble"

/** Test tag of the menu item that opens the time zone choice for a file, suffixed with the source id. */
const val SOURCE_MENU_ZONE_TAG = "source-menu-zone"

/**
 * One imported file: click to show or hide its records inside the merged session.
 *
 * Showing and hiding is what the chip is for, so the whole chip keeps that click. Everything else
 * that can be done to one file lives in a menu of its own, reached by a secondary click anywhere on
 * the chip or by the small trigger at its end. The trigger is there because a menu known only to a
 * right click is a menu most people never find; it is drawn in the metadata colour and without a
 * border of its own, so the file's name stays the loudest thing on the chip.
 *
 * The menu is built to hold several entries. It is drawn only when at least one of them applies to
 * this file, so a chip with nothing to offer does not promise a menu that opens empty.
 *
 * The file's time zone is named in the subtitle only when it says something: a zone other than the
 * assumed UTC always does, and the assumed UTC does once [isZoneWorthNaming] — once another file of
 * the session runs in a different zone, "UTC" stops being the obvious reading and becomes a fact.
 */
@Composable
fun SourceChip(
    source: LogSourceUi,
    onClick: () -> Unit,
    onIntent: (LogPlayerIntent) -> Unit,
    isZoneWorthNaming: Boolean = false,
) {
    var isMenuOpen by remember(key1 = source.id) { mutableStateOf(value = false) }
    val hasMenu = source.hasPreamble || source.isZoneChangeable

    Box {
        Row(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.small)
                .fill(color = if (source.isSelected) MaterialTheme.colorScheme.surfaceVariant else null)
                .border(
                    width = BORDER_WIDTH.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small,
                )
                .onSecondaryPress(isEnabled = hasMenu) { isMenuOpen = true }
                .clickable(onClick = onClick)
                .padding(
                    start = Spacing.small,
                    end = if (hasMenu) Spacing.tight else Spacing.small,
                    top = Spacing.tight,
                    bottom = Spacing.tight,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = Spacing.tight),
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
                    text = sourceSubtitle(source = source, isZoneWorthNaming = isZoneWorthNaming),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            if (hasMenu) {
                SourceMenuTrigger(source = source, onOpen = { isMenuOpen = true })
            }
        }

        SourceMenu(
            source = source,
            isOpen = isMenuOpen,
            onClose = { isMenuOpen = false },
            onIntent = onIntent,
        )
    }
}

@Composable
private fun SourceMenuTrigger(source: LogSourceUi, onOpen: () -> Unit) {
    val description = stringResource(resource = Res.string.source_chip_menu_description, source.name)
    Box(
        modifier = Modifier
            .clip(shape = MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = description }
            .testTag(tag = "$SOURCE_CHIP_MENU_TAG-${source.id}")
            .padding(horizontal = Spacing.tight, vertical = Spacing.hairline),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(resource = Res.string.source_chip_menu_glyph),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * What can be done to one file beyond showing or hiding it. Each entry decides for itself whether it
 * applies; the chip draws its trigger only when one does.
 */
@Composable
private fun SourceMenu(
    source: LogSourceUi,
    isOpen: Boolean,
    onClose: () -> Unit,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    AppDropdownMenu(expanded = isOpen, onDismissRequest = onClose) {
        if (source.hasPreamble) {
            DropdownMenuItem(
                text = { Text(text = stringResource(resource = Res.string.source_menu_preamble)) },
                onClick = {
                    onClose()
                    onIntent(LogPlayerIntent.ShowSourcePreamble(sourceId = source.id))
                },
                modifier = Modifier.testTag(tag = "$SOURCE_MENU_PREAMBLE_TAG-${source.id}"),
            )
        }
        // Absent rather than disabled for a file whose lines carry their own offset: there is
        // nothing to choose, and the subtitle already says where its zone came from.
        if (source.isZoneChangeable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(resource = Res.string.source_menu_zone)) },
                onClick = {
                    onClose()
                    onIntent(LogPlayerIntent.RequestSourceZone(sourceId = source.id))
                },
                modifier = Modifier.testTag(tag = "$SOURCE_MENU_ZONE_TAG-${source.id}"),
            )
        }
    }
}

/** A zone read off the records themselves is a fact about the file, not a choice. */
private val LogSourceUi.isZoneChangeable: Boolean
    get() = zone.origin != ZoneOrigin.LINE

/**
 * Opens on a press of the secondary button, the desktop convention for "what can I do with this".
 * The press is consumed so that the chip's own click does not also toggle the file.
 */
private fun Modifier.onSecondaryPress(isEnabled: Boolean, onPress: () -> Unit): Modifier =
    if (!isEnabled) {
        this
    } else {
        pointerInput(key1 = onPress) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { change -> change.consume() }
                        onPress()
                    }
                }
            }
        }
    }

@Composable
private fun sourceSubtitle(source: LogSourceUi, isZoneWorthNaming: Boolean): String {
    val skipped = if (source.skippedLineCount > 0) {
        stringResource(resource = Res.string.source_chip_skipped, source.skippedLineCount)
    } else {
        ""
    }
    val counts = stringResource(
        resource = Res.string.source_chip_subtitle,
        source.entryCount,
        source.formatName,
    ) + skipped
    // Text, not a dot or a colour: the header is a fact about the file, stated where the other
    // facts about it are. A zone taken from the header already says there is one, so it is not
    // said twice.
    // A merged source says how many files it holds; a single file, the usual case, says nothing.
    val files = if (source.fileCount > 1) {
        stringResource(
            resource = Res.string.source_chip_with_files,
            counts,
            pluralStringResource(
                resource = Res.plurals.source_chip_file_count,
                quantity = source.fileCount,
                source.fileCount,
            ),
        )
    } else {
        counts
    }
    val facts = if (source.hasPreamble && source.zone.origin != ZoneOrigin.HEADER) {
        stringResource(resource = Res.string.source_chip_with_preamble, files)
    } else {
        files
    }
    val zone = zoneFact(source = source, isZoneWorthNaming = isZoneWorthNaming)
    return if (zone == null) facts else stringResource(resource = Res.string.source_chip_with_zone, facts, zone)
}

/** Where the file's clock runs, and where that came from when it did not come from the user. */
@Composable
private fun zoneFact(source: LogSourceUi, isZoneWorthNaming: Boolean): String? = when (source.zone.origin) {
    ZoneOrigin.LINE -> stringResource(resource = Res.string.source_zone_line, source.zone.id)
    ZoneOrigin.HEADER -> stringResource(resource = Res.string.source_zone_header, source.zone.id)
    ZoneOrigin.CHOSEN -> source.zone.id
    ZoneOrigin.DEFAULT -> if (isZoneWorthNaming) stringResource(resource = Res.string.source_zone_assumed) else null
}
