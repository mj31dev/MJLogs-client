package dev.mj31.logger.client.app.features.logplayer.format.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_delimited_level_label
import dev.mj31.logger.client.app.resources.format_delimited_message_label
import dev.mj31.logger.client.app.resources.format_delimited_tag_label
import dev.mj31.logger.client.app.resources.format_delimited_timestamp_label
import dev.mj31.logger.client.app.resources.format_delimiter_label
import dev.mj31.logger.client.app.resources.format_header_label
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import org.jetbrains.compose.resources.stringResource

/**
 * A row of separated columns, addressed by header name or by position.
 *
 * The separator and the header question stand on their own line above the columns because they
 * decide what a column even is: without a header row, a name in any of the four boxes below means
 * nothing.
 */
@Composable
internal fun DelimitedFormatInputs(
    request: FormatRequestUiState,
    draft: ManualFormatInput.Delimited,
    timestampPattern: String,
    onDraftChange: (ManualFormatInput) -> Unit,
) {
    val delimiter = rememberTextDraft(external = draft.delimiter, resetKey = request.path)
    val timestampField = rememberTextDraft(external = draft.timestampField, resetKey = request.path)
    val levelField = rememberTextDraft(external = draft.levelField, resetKey = request.path)
    val tagField = rememberTextDraft(external = draft.tagField, resetKey = request.path)
    val messageField = rememberTextDraft(external = draft.messageField, resetKey = request.path)

    fun report(hasHeader: Boolean = draft.hasHeader) = onDraftChange(
        draft.copy(
            timestampPattern = timestampPattern,
            delimiter = delimiter.value,
            hasHeader = hasHeader,
            timestampField = timestampField.value,
            levelField = levelField.value,
            tagField = tagField.value,
            messageField = messageField.value,
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = Spacing.small),
    ) {
        FormatFieldPair {
            FormatField(
                state = delimiter,
                label = Res.string.format_delimiter_label,
                error = request.errorFor(field = FormatErrorField.DELIMITER),
                onTyped = { report() },
                modifier = Modifier.weight(weight = 1f),
            )
            HeaderToggle(
                checked = draft.hasHeader,
                onCheckedChange = { checked -> report(hasHeader = checked) },
                modifier = Modifier.weight(weight = 1f),
            )
        }
        FormatFieldPair {
            FormatField(
                state = timestampField,
                label = Res.string.format_delimited_timestamp_label,
                error = request.errorFor(field = FormatErrorField.TIMESTAMP_FIELD),
                onTyped = { report() },
                modifier = Modifier.weight(weight = 1f),
            )
            FormatField(
                state = levelField,
                label = Res.string.format_delimited_level_label,
                error = request.errorFor(field = FormatErrorField.LEVEL_FIELD),
                onTyped = { report() },
                modifier = Modifier.weight(weight = 1f),
            )
        }
        FormatFieldPair {
            FormatField(
                state = tagField,
                label = Res.string.format_delimited_tag_label,
                error = request.errorFor(field = FormatErrorField.TAG_FIELD),
                onTyped = { report() },
                modifier = Modifier.weight(weight = 1f),
            )
            FormatField(
                state = messageField,
                label = Res.string.format_delimited_message_label,
                error = request.errorFor(field = FormatErrorField.MESSAGE_FIELD),
                onTyped = { report() },
                modifier = Modifier.weight(weight = 1f),
            )
        }
    }
}

/** Sits beside the separator box, and is padded down to the height a box of that row stands at. */
@Composable
private fun HeaderToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(top = Spacing.small)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(resource = Res.string.format_header_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.tight),
        )
    }
}
