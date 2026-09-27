package dev.mj31.logger.client.app.view.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The one menu of the application: a `surface` with an `outline`, and no shadow.
 *
 * Material's menu casts one by default, and a menu opens over the log list and near the frame,
 * where a shadow reads as a rendering artefact. Depth is a change of surface colour here like
 * everywhere else, and the outline marks where the menu meets the surface it opened over. Screens use
 * this rather than `DropdownMenu`, so the rule holds without being remembered.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
        content = content,
    )
}
