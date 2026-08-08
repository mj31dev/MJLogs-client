package dev.mj31.logger.client.app.features.logplayer.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.sync_frame_time_picker_cancel
import dev.mj31.logger.client.app.resources.sync_frame_time_picker_confirm
import dev.mj31.logger.client.app.resources.sync_frame_time_picker_hint
import dev.mj31.logger.client.app.resources.sync_frame_time_picker_title
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Date and time of the current frame, picked with the mouse.
 *
 * The picker deliberately stops at minutes: Material3 offers nothing finer, and a log record is
 * located to the millisecond, so the seconds stay in the text field the dialog writes back into.
 * [initial] is where it opens, which is the readable content of that field or the session start.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameTimePickerDialog(
    initial: Instant?,
    onDismiss: () -> Unit,
    onConfirm: (dateMillis: Long, hour: Int, minute: Int) -> Unit,
) {
    val moment = (initial ?: Instant.fromEpochMilliseconds(epochMilliseconds = 0L))
        .toLocalDateTime(timeZone = TimeZone.UTC)
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toEpochMilliseconds() ?: 0L,
    )
    val timeState = rememberTimePickerState(
        initialHour = moment.hour,
        initialMinute = moment.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.sync_frame_time_picker_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                Text(
                    text = stringResource(resource = Res.string.sync_frame_time_picker_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
                DatePicker(state = dateState, title = null, showModeToggle = false)
                Spacer(modifier = Modifier.height(height = 8.dp))
                TimeInput(state = timeState, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        onConfirm(millis, timeState.hour, timeState.minute)
                    }
                },
                enabled = dateState.selectedDateMillis != null,
            ) {
                Text(text = stringResource(resource = Res.string.sync_frame_time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.sync_frame_time_picker_cancel))
            }
        },
    )
}
