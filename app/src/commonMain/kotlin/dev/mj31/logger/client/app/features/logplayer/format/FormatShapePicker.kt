package dev.mj31.logger.client.app.features.logplayer.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatKind
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_shape_delimited
import dev.mj31.logger.client.app.resources.format_shape_json
import dev.mj31.logger.client.app.resources.format_shape_label
import dev.mj31.logger.client.app.resources.format_shape_template
import dev.mj31.logger.client.app.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the three shapes the file is being described as.
 *
 * The three are shown side by side rather than hidden in a menu: detection guesses the shape and
 * guesses it wrong exactly when this dialog opens, so the alternatives have to be visible at the
 * moment the user reads that the guess failed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormatShapePicker(
    selected: FormatKind,
    onSelect: (FormatKind) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
        Text(
            text = stringResource(resource = Res.string.format_shape_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            FormatKind.entries.forEachIndexed { index, kind ->
                SegmentedButton(
                    selected = kind == selected,
                    onClick = { onSelect(kind) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = FormatKind.entries.size),
                ) {
                    Text(text = stringResource(resource = labelOf(kind = kind)))
                }
            }
        }
    }
}

private fun labelOf(kind: FormatKind): StringResource = when (kind) {
    FormatKind.TEMPLATE -> Res.string.format_shape_template
    FormatKind.JSON -> Res.string.format_shape_json
    FormatKind.DELIMITED -> Res.string.format_shape_delimited
}
