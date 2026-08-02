package dev.mj31.logger.client.app.features.logplayer.format

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mj31.logger.client.app.theme.AccentSync
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.view.FormatPreviewView
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_apply
import dev.mj31.logger.client.app.resources.format_dialog_confirm_title
import dev.mj31.logger.client.app.resources.format_dialog_title
import dev.mj31.logger.client.app.resources.format_help
import dev.mj31.logger.client.app.resources.format_import_as_detected
import dev.mj31.logger.client.app.resources.format_preset_custom_prefix
import dev.mj31.logger.client.app.resources.format_preset_epoch
import dev.mj31.logger.client.app.resources.format_preset_iso
import dev.mj31.logger.client.app.resources.format_preset_logcat
import dev.mj31.logger.client.app.resources.format_preset_time_only
import dev.mj31.logger.client.app.resources.format_skip_file
import dev.mj31.logger.client.app.resources.format_structure_label
import dev.mj31.logger.client.app.resources.format_suggestion_hint
import dev.mj31.logger.client.app.resources.format_timestamp_label
import dev.mj31.logger.client.app.resources.format_use_mine
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val HELP_FONT_SIZE = 11

private data class FormatPreset(
    val label: StringResource,
    val timestampPattern: String,
    val structureTemplate: String,
)

private val presets: List<FormatPreset> = listOf(
    FormatPreset(
        label = Res.string.format_preset_iso,
        timestampPattern = "yyyy-MM-dd HH:mm:ss.SSS",
        structureTemplate = "{timestamp} {level} {tag}: {message}",
    ),
    FormatPreset(
        label = Res.string.format_preset_logcat,
        timestampPattern = "MM-dd HH:mm:ss.SSS",
        structureTemplate = "{timestamp} {level}/{tag}: {message}",
    ),
    FormatPreset(
        label = Res.string.format_preset_time_only,
        timestampPattern = "HH:mm:ss",
        structureTemplate = "{timestamp} {message}",
    ),
    FormatPreset(
        label = Res.string.format_preset_epoch,
        timestampPattern = "epochMillis",
        structureTemplate = "{timestamp} {level} {message}",
    ),
    FormatPreset(
        label = Res.string.format_preset_custom_prefix,
        timestampPattern = "dd.MM.yyyy_HH.mm.ss",
        structureTemplate = "<{any}>~{timestamp}~{tag}~{message}",
    ),
)

/**
 * Asks the user to describe a log layout the detector could not recognize.
 *
 * The dialog stays open on failure so the pattern can be corrected against the sample lines shown
 * right above the inputs.
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
        text = {
            WizardContent(
                request = request,
                onDraftChange = { timestampPattern, structureTemplate ->
                    onIntent(
                        LogPlayerIntent.UpdateFormatDraft(
                            timestampPattern = timestampPattern,
                            structureTemplate = structureTemplate,
                        ),
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
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
            Row {
                // A file that already parsed is kept, not dropped, when the user declines to edit.
                if (request.isConfirmation) {
                    TextButton(onClick = { onIntent(LogPlayerIntent.AcceptDetectedFormat) }) {
                        Text(text = stringResource(resource = Res.string.format_import_as_detected))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(resource = Res.string.format_skip_file))
                }
            }
        },
    )
}

@Composable
private fun WizardContent(
    request: FormatRequestUiState,
    onDraftChange: (timestampPattern: String, structureTemplate: String) -> Unit,
) {
    val timestampDraft = rememberTextDraft(external = request.timestampPattern, resetKey = request.path)
    val structureDraft = rememberTextDraft(external = request.structureTemplate, resetKey = request.path)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = request.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (request.suggestion != null) {
            Spacer(modifier = Modifier.height(height = 10.dp))
            Text(
                text = stringResource(resource = Res.string.format_suggestion_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AccentSync,
            )
        }

        Spacer(modifier = Modifier.height(height = 12.dp))

        PresetRow(
            onPresetSelected = { preset ->
                onDraftChange(preset.timestampPattern, preset.structureTemplate)
            },
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

        OutlinedTextField(
            value = timestampDraft.value,
            onValueChange = { typed ->
                timestampDraft.value = typed
                onDraftChange(typed, structureDraft.value)
            },
            label = { Text(text = stringResource(resource = Res.string.format_timestamp_label)) },
            singleLine = true,
            isError = request.timestampPatternError != null,
            supportingText = request.timestampPatternError?.let { error -> { FieldError(message = error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

        OutlinedTextField(
            value = structureDraft.value,
            onValueChange = { typed ->
                structureDraft.value = typed
                onDraftChange(timestampDraft.value, typed)
            },
            label = { Text(text = stringResource(resource = Res.string.format_structure_label)) },
            singleLine = true,
            isError = request.structureTemplateError != null,
            supportingText = request.structureTemplateError?.let { error -> { FieldError(message = error) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 10.dp))

        FormatPreviewView(preview = request.preview)

        Spacer(modifier = Modifier.height(height = 8.dp))

        HelpText()

        // Fallback for a failure that belongs to no input; rendered in place, never as a native dialog.
        request.generalError?.let { error ->
            Spacer(modifier = Modifier.height(height = 8.dp))
            ErrorNotice(message = error)
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/** In-place error banner: the wizard is already a dialog, a second one would only get in the way. */
@Composable
private fun ErrorNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 6.dp))
            .background(color = MaterialTheme.colorScheme.errorContainer)
            .padding(all = 10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun PresetRow(onPresetSelected: (FormatPreset) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 4.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPresetSelected(preset) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(resource = preset.label),
                    fontSize = HELP_FONT_SIZE.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HelpText() {
    Text(
        text = stringResource(resource = Res.string.format_help),
        fontSize = HELP_FONT_SIZE.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
