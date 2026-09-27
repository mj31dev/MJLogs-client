package dev.mj31.logger.client.app.features.logplayer.format.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.format_json_level_label
import dev.mj31.logger.client.app.resources.format_json_message_label
import dev.mj31.logger.client.app.resources.format_json_tag_label
import dev.mj31.logger.client.app.resources.format_json_timestamp_label
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * A line holding one JSON object, described by the key each component lives under.
 *
 * The four keys are laid out two by two: they are one question asked four times, and a column of
 * four full width boxes would push the sample lines they are checked against off the dialog.
 */
@Composable
internal fun JsonFormatInputs(
    request: FormatRequestUiState,
    draft: ManualFormatInput.Json,
    timestampPattern: String,
    onDraftChange: (ManualFormatInput) -> Unit,
) {
    val timestampKey = rememberTextDraft(external = draft.timestampKey, resetKey = request.path)
    val levelKey = rememberTextDraft(external = draft.levelKey, resetKey = request.path)
    val tagKey = rememberTextDraft(external = draft.tagKey, resetKey = request.path)
    val messageKey = rememberTextDraft(external = draft.messageKey, resetKey = request.path)

    fun report() = onDraftChange(
        draft.copy(
            timestampPattern = timestampPattern,
            timestampKey = timestampKey.value,
            levelKey = levelKey.value,
            tagKey = tagKey.value,
            messageKey = messageKey.value,
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = Spacing.small),
    ) {
        FormatFieldPair {
            FormatField(
                state = timestampKey,
                label = Res.string.format_json_timestamp_label,
                error = request.errorFor(field = FormatErrorField.TIMESTAMP_FIELD),
                onTyped = ::report,
                modifier = Modifier.weight(weight = 1f),
            )
            FormatField(
                state = levelKey,
                label = Res.string.format_json_level_label,
                error = request.errorFor(field = FormatErrorField.LEVEL_FIELD),
                onTyped = ::report,
                modifier = Modifier.weight(weight = 1f),
            )
        }
        FormatFieldPair {
            FormatField(
                state = tagKey,
                label = Res.string.format_json_tag_label,
                error = request.errorFor(field = FormatErrorField.TAG_FIELD),
                onTyped = ::report,
                modifier = Modifier.weight(weight = 1f),
            )
            FormatField(
                state = messageKey,
                label = Res.string.format_json_message_label,
                error = request.errorFor(field = FormatErrorField.MESSAGE_FIELD),
                onTyped = ::report,
                modifier = Modifier.weight(weight = 1f),
            )
        }
    }
}
