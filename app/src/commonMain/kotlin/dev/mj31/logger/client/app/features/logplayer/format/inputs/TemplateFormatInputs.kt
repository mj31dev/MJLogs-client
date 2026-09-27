package dev.mj31.logger.client.app.features.logplayer.format.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_preset_custom_prefix
import dev.mj31.logger.client.app.resources.format_preset_epoch
import dev.mj31.logger.client.app.resources.format_preset_iso
import dev.mj31.logger.client.app.resources.format_preset_logcat
import dev.mj31.logger.client.app.resources.format_preset_time_only
import dev.mj31.logger.client.app.resources.format_structure_label
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
 * A line described by literals and placeholders — the shape most log files still have.
 *
 * The presets sit above the box rather than beside it: they replace both the pattern and the
 * structure at once, so they are a starting point for the whole description and not an edit to one
 * field of it.
 */
@Composable
internal fun TemplateFormatInputs(
    request: FormatRequestUiState,
    draft: ManualFormatInput.Template,
    timestampPattern: String,
    onDraftChange: (ManualFormatInput) -> Unit,
) {
    val structureDraft = rememberTextDraft(external = draft.structureTemplate, resetKey = request.path)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = Spacing.small),
    ) {
        PresetRow(
            onPresetSelected = { preset ->
                onDraftChange(
                    ManualFormatInput.Template(
                        timestampPattern = preset.timestampPattern,
                        structureTemplate = preset.structureTemplate,
                    ),
                )
            },
        )

        FormatField(
            state = structureDraft,
            label = Res.string.format_structure_label,
            error = request.structureTemplateError,
            onTyped = {
                onDraftChange(
                    draft.copy(
                        timestampPattern = timestampPattern,
                        structureTemplate = structureDraft.value,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PresetRow(onPresetSelected: (FormatPreset) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = Spacing.small),
    ) {
        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.small)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPresetSelected(preset) }
                    .padding(horizontal = Spacing.small, vertical = Spacing.tight),
            ) {
                Text(
                    text = stringResource(resource = preset.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
