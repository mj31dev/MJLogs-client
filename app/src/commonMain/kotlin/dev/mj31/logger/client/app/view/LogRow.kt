package dev.mj31.logger.client.app.view

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mj31.logger.client.app.theme.AccentActive
import dev.mj31.logger.client.app.theme.AccentSync
import dev.mj31.logger.client.app.theme.LocalLogLevelColors
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.app.view.format.formatLogTime

private const val ROW_FONT_SIZE = 12
private const val TIME_COLUMN_WIDTH = 86
private const val TAG_COLUMN_WIDTH = 128
private const val LEVEL_COLUMN_WIDTH = 22
private const val SELECTED_TINT_ALPHA = 0.35f
private const val ACTIVE_TINT_ALPHA = 0.22f

/**
 * One record of the merged session.
 *
 * "Active" means the video playhead currently stands on this record, "selected" means the user
 * picked it (and therefore that it can be used as a synchronization anchor).
 */
@Composable
fun LogRow(
    entry: LogEntry,
    isSelected: Boolean,
    isActive: Boolean,
    isAnchor: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val levelColor = LocalLogLevelColors.current.of(level = entry.level)
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_TINT_ALPHA)
        isActive -> AccentActive.copy(alpha = ACTIVE_TINT_ALPHA)
        else -> Color.Transparent
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
            .background(color = background)
            .padding(vertical = 2.dp, horizontal = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(width = if (isActive || isAnchor) 4.dp else 2.dp)
                .height(height = 16.dp)
                .clip(shape = RoundedCornerShape(size = 2.dp))
                .background(color = markerColor),
        )

        Spacer(modifier = Modifier.width(width = 8.dp))

        Text(
            text = formatLogTime(instant = entry.timestamp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = ROW_FONT_SIZE.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(width = TIME_COLUMN_WIDTH.dp),
        )

        Text(
            text = entry.level.name.take(n = 1),
            color = levelColor,
            fontSize = ROW_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(width = LEVEL_COLUMN_WIDTH.dp),
        )

        Text(
            text = entry.tag,
            color = MaterialTheme.colorScheme.primary,
            fontSize = ROW_FONT_SIZE.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier.width(width = TAG_COLUMN_WIDTH.dp),
        )

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = entry.message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = ROW_FONT_SIZE.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
