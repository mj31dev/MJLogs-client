package dev.mj31.logger.client.app.view

import dev.mj31.logger.client.app.theme.type.ContentType
import dev.mj31.logger.client.app.view.modifier.fill
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.theme.AccentActive
import dev.mj31.logger.client.app.theme.AccentSync
import dev.mj31.logger.client.app.theme.LocalLogLevelColors
import dev.mj31.logger.client.app.theme.Spacing
import kotlinx.datetime.TimeZone
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.app.view.format.formatLogTime

private const val MARKER_WIDTH = 2
private const val MARKER_EMPHASIZED_WIDTH = 4
private const val MARKER_HEIGHT = 16
private const val TIME_COLUMN_WIDTH = 100
private const val UTC_COLUMN_WIDTH = 108
private const val TAG_COLUMN_WIDTH = 128
private const val LEVEL_COLUMN_WIDTH = 22
private const val SELECTED_TINT_ALPHA = 0.35f
private const val ACTIVE_TINT_ALPHA = 0.22f
private const val UTC_TIME_ALPHA = 0.7f

/** Marks the second time column as UTC in the text itself, so the colour is not the only carrier. */
private const val UTC_DESIGNATOR = "Z"

/**
 * One record of the merged session.
 *
 * "Active" means the video playhead currently stands on this record, "selected" means the user
 * picked it (and therefore that it can be used as a synchronization anchor).
 *
 * The first time is the record's own, in [timeZone] — the zone of the file it came from — so it
 * reads exactly as the file wrote it. When [showUtcTime] is set the same moment follows in UTC,
 * quieter and suffixed `Z`: files from different zones interleave by that column, and without it a
 * merged session looks out of order wherever two zones meet.
 */
@Composable
fun LogRow(
    entry: LogEntry,
    isSelected: Boolean,
    isActive: Boolean,
    isAnchor: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.UTC,
    showUtcTime: Boolean = false,
) {
    val levelColor = LocalLogLevelColors.current.of(level = entry.level)
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_TINT_ALPHA)
        isActive -> AccentActive.copy(alpha = ACTIVE_TINT_ALPHA)
        else -> null
    }
    val markerColor = when {
        isAnchor -> AccentSync
        isActive -> AccentActive
        else -> levelColor
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .fill(color = background)
            .padding(vertical = Spacing.hairline, horizontal = Spacing.hairline),
    ) {
        Box(
            modifier = Modifier
                .width(width = if (isActive || isAnchor) MARKER_EMPHASIZED_WIDTH.dp else MARKER_WIDTH.dp)
                .height(height = MARKER_HEIGHT.dp)
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .background(color = markerColor),
        )

        Spacer(modifier = Modifier.width(width = Spacing.small))

        TimeColumns(entry = entry, timeZone = timeZone, showUtcTime = showUtcTime)

        Text(
            text = entry.level.name.take(n = 1),
            color = levelColor,
            style = ContentType.record,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(width = LEVEL_COLUMN_WIDTH.dp),
        )

        Text(
            text = entry.tag,
            color = MaterialTheme.colorScheme.primary,
            style = ContentType.record,
            maxLines = 1,
            modifier = Modifier.width(width = TAG_COLUMN_WIDTH.dp),
        )

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = entry.message,
                color = MaterialTheme.colorScheme.onSurface,
                style = ContentType.record,
            )
        }
    }
}

/** The record's own time, then — when files from other zones share the session — the same in UTC. */
@Composable
private fun TimeColumns(entry: LogEntry, timeZone: TimeZone, showUtcTime: Boolean) {
    Text(
        text = formatLogTime(instant = entry.timestamp, timeZone = timeZone),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = ContentType.record,
        maxLines = 1,
        modifier = Modifier.width(width = TIME_COLUMN_WIDTH.dp),
    )

    if (showUtcTime) {
        Text(
            text = formatLogTime(instant = entry.timestamp) + UTC_DESIGNATOR,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UTC_TIME_ALPHA),
            style = ContentType.record,
            maxLines = 1,
            modifier = Modifier.width(width = UTC_COLUMN_WIDTH.dp),
        )
    }
}
