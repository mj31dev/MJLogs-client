package dev.mj31.logger.client.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_dismiss
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.app.view.text.resolve
import org.jetbrains.compose.resources.stringResource

/**
 * Transient notice shown at the bottom of the workspace; [isError] only drives its colours.
 *
 * It is the application wide fallback for anything that cannot be attached to a specific input.
 */
data class UiMessage(
    val text: UiText,
    val isError: Boolean = false,
)

/**
 * The application wide fallback notice: a composable bar in the window itself, deliberately not a
 * native modal, so it never steals focus from what the user is doing.
 *
 * It floats over the workspace rather than sitting in the column with it. A notice that takes part
 * in the layout resizes everything below it the instant it appears and again when it goes, and the
 * two panes it shifts are the ones the user is reading — so a message about something that just
 * happened would move the very thing it is describing. Hence the shadow: it has to read as being
 * above the content, not spliced into it.
 */
@Composable
fun MessageBar(message: UiMessage, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val background = if (message.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (message.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shadowElevation = ELEVATION.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message.text.resolve(),
                style = MaterialTheme.typography.bodySmall,
                color = foreground,
                modifier = Modifier.weight(weight = 1f),
            )
            TextButton(onClick = onDismiss) { Text(text = stringResource(resource = Res.string.message_dismiss)) }
        }
    }
}

private const val ELEVATION = 6
