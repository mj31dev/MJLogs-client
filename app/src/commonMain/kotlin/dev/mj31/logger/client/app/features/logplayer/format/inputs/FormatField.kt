package dev.mj31.logger.client.app.features.logplayer.format.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.mj31.logger.client.app.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One box of the wizard, with the message that belongs to it underneath.
 *
 * All three shapes are described through boxes that differ only in their label, so one composable
 * draws them: an error then always appears in the same place relative to the input that caused it,
 * whichever set of inputs is on screen.
 *
 * [state] is the locally held text, not the value from the store — see `rememberTextDraft` for why —
 * and [onTyped] reports the whole draft once the box has taken the keystroke.
 */
@Composable
internal fun FormatField(
    state: MutableState<String>,
    label: StringResource,
    error: String?,
    onTyped: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: StringResource? = null,
    hint: StringResource? = null,
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { typed ->
            state.value = typed
            onTyped()
        },
        label = { Text(text = stringResource(resource = label)) },
        placeholder = placeholder?.let { resource -> { Text(text = stringResource(resource = resource)) } },
        singleLine = true,
        isError = error != null,
        // A hint holds the line an error would take, so the error replaces it rather than adding one.
        supportingText = when {
            error != null -> { { FieldError(message = error) } }
            hint != null -> { { FieldHint(message = stringResource(resource = hint)) } }
            else -> null
        },
        modifier = modifier,
    )
}

/**
 * Two boxes side by side, which is how the four components of a record are asked about.
 *
 * They are aligned to the top rather than centred, so a message under one of them moves nothing but
 * itself.
 */
@Composable
internal fun FormatFieldPair(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = Spacing.small),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

@Composable
private fun FieldHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FieldError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
