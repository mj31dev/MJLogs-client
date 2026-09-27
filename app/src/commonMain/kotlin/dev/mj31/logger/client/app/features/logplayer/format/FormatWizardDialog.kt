package dev.mj31.logger.client.app.features.logplayer.format

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.format.inputs.DelimitedFormatInputs
import dev.mj31.logger.client.app.features.logplayer.format.inputs.FormatField
import dev.mj31.logger.client.app.features.logplayer.format.inputs.JsonFormatInputs
import dev.mj31.logger.client.app.features.logplayer.format.inputs.TemplateFormatInputs
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatKind
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_apply
import dev.mj31.logger.client.app.resources.format_dialog_confirm_title
import dev.mj31.logger.client.app.resources.format_dialog_title
import dev.mj31.logger.client.app.resources.format_help_delimited
import dev.mj31.logger.client.app.resources.format_help_json
import dev.mj31.logger.client.app.resources.format_help_template
import dev.mj31.logger.client.app.resources.format_help_timestamp
import dev.mj31.logger.client.app.resources.format_import_as_detected
import dev.mj31.logger.client.app.resources.format_skip_file
import dev.mj31.logger.client.app.resources.format_suggestion_hint
import dev.mj31.logger.client.app.resources.format_timestamp_label
import dev.mj31.logger.client.app.resources.format_use_mine
import dev.mj31.logger.client.app.resources.format_zone_hint
import dev.mj31.logger.client.app.resources.format_zone_label
import dev.mj31.logger.client.app.resources.format_zone_placeholder
import androidx.compose.ui.platform.testTag
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.FormatPreviewView
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Test tag of the box naming the zone the file's clock runs in. */
const val FORMAT_ZONE_FIELD_TAG = "format-zone-field"

/**
 * Asks the user to describe a log the detector could not read, in whichever of the three shapes it
 * has.
 *
 * The dialog stays open on failure so the description can be corrected against the sample lines,
 * which are shown right under the shape it is being read as: the preview is the evidence this screen
 * is trusted for, so it sits above the inputs rather than at the end of them.
 */
@Composable
fun FormatWizardDialog(
    request: FormatRequestUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onDismiss = { onIntent(LogPlayerIntent.DismissFormatRequest) }
    val title = stringResource(
        resource = if (request.isConfirmation) {
            Res.string.format_dialog_confirm_title
        } else {
            Res.string.format_dialog_title
        },
        request.fileName,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { WizardContent(request = request, onIntent = onIntent) },
        confirmButton = {
            // The one thing this dialog exists for: reading the file the way it is described here.
            Button(
                onClick = { onIntent(LogPlayerIntent.SubmitManualFormat) },
                enabled = request.canApply,
            ) {
                Text(
                    text = stringResource(
                        resource = if (request.isConfirmation) Res.string.format_use_mine else Res.string.format_apply,
                    ),
                )
            }
        },
        dismissButton = {
            // Left to right by weight: dropping the file, keeping it as read, taking what was typed.
            Row(horizontalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(resource = Res.string.format_skip_file))
                }
                // A file that already parsed is kept, not dropped, when the user declines to edit.
                if (request.isConfirmation) {
                    OutlinedButton(onClick = { onIntent(LogPlayerIntent.AcceptDetectedFormat) }) {
                        Text(text = stringResource(resource = Res.string.format_import_as_detected))
                    }
                }
            }
        },
    )
}

@Composable
private fun WizardContent(
    request: FormatRequestUiState,
    onIntent: (LogPlayerIntent) -> Unit,
) {
    val onDraftChange: (ManualFormatInput) -> Unit = { draft ->
        onIntent(LogPlayerIntent.UpdateFormatDraft(draft = draft))
    }
    val timestampDraft = rememberTextDraft(external = request.draft.timestampPattern, resetKey = request.path)
    val zoneDraft = rememberTextDraft(external = request.draft.zoneId, resetKey = request.path)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = Spacing.large),
    ) {
        Introduction(request = request)

        FormatShapePicker(
            selected = request.kind,
            onSelect = { kind -> onIntent(LogPlayerIntent.SelectFormatKind(kind = kind)) },
        )

        // Fallback for a failure that belongs to no input; it explains the preview under it.
        request.generalError?.let { error -> ErrorNotice(message = error) }

        FormatPreviewView(preview = request.preview, sampleLines = request.sampleLines)

        Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
            // The two fields all three shapes share, so they stand above the ones only one of them has.
            FormatField(
                state = timestampDraft,
                label = Res.string.format_timestamp_label,
                error = request.timestampPatternError,
                onTyped = { onDraftChange(request.draft.withTimestampPattern(pattern = timestampDraft.value)) },
                modifier = Modifier.fillMaxWidth(),
            )
            // Beside the pattern because it qualifies it: the same digits are another moment in
            // another zone.
            FormatField(
                state = zoneDraft,
                label = Res.string.format_zone_label,
                error = request.errorFor(field = FormatErrorField.ZONE),
                onTyped = { onDraftChange(request.draft.withZoneId(zoneId = zoneDraft.value)) },
                placeholder = Res.string.format_zone_placeholder,
                hint = Res.string.format_zone_hint,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag = FORMAT_ZONE_FIELD_TAG),
            )
            ShapeInputs(
                request = request,
                timestampPattern = timestampDraft.value,
                onDraftChange = onDraftChange,
            )
        }

        HelpText(kind = request.kind)
    }
}

@Composable
private fun Introduction(request: FormatRequestUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
        Text(
            text = request.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (request.suggestion != null) {
            Text(
                text = stringResource(resource = Res.string.format_suggestion_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ShapeInputs(
    request: FormatRequestUiState,
    timestampPattern: String,
    onDraftChange: (ManualFormatInput) -> Unit,
) {
    when (val draft = request.draft) {
        is ManualFormatInput.Template -> TemplateFormatInputs(
            request = request,
            draft = draft,
            timestampPattern = timestampPattern,
            onDraftChange = onDraftChange,
        )

        is ManualFormatInput.Json -> JsonFormatInputs(
            request = request,
            draft = draft,
            timestampPattern = timestampPattern,
            onDraftChange = onDraftChange,
        )

        is ManualFormatInput.Delimited -> DelimitedFormatInputs(
            request = request,
            draft = draft,
            timestampPattern = timestampPattern,
            onDraftChange = onDraftChange,
        )
    }
}

/** In-place error banner: the wizard is already a dialog, a second one would only get in the way. */
@Composable
private fun ErrorNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.errorContainer)
            .padding(all = Spacing.medium),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** The tokens are the same for every shape; only what surrounds them changes. */
@Composable
private fun HelpText(kind: FormatKind) {
    Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline)) {
        Text(
            text = stringResource(resource = Res.string.format_help_timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(resource = helpOf(kind = kind)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun helpOf(kind: FormatKind): StringResource = when (kind) {
    FormatKind.TEMPLATE -> Res.string.format_help_template
    FormatKind.JSON -> Res.string.format_help_json
    FormatKind.DELIMITED -> Res.string.format_help_delimited
}

/** The zone is shared by every shape, as the pattern is. */
private fun ManualFormatInput.withZoneId(zoneId: String): ManualFormatInput = when (this) {
    is ManualFormatInput.Template -> copy(zoneId = zoneId)
    is ManualFormatInput.Json -> copy(zoneId = zoneId)
    is ManualFormatInput.Delimited -> copy(zoneId = zoneId)
}

/** The one value that survives a change of shape, so it is carried rather than retyped. */
private fun ManualFormatInput.withTimestampPattern(pattern: String): ManualFormatInput = when (this) {
    is ManualFormatInput.Template -> copy(timestampPattern = pattern)
    is ManualFormatInput.Json -> copy(timestampPattern = pattern)
    is ManualFormatInput.Delimited -> copy(timestampPattern = pattern)
}
