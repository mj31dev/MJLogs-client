package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.sync.AutoSyncMenu
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.app.view.format.formatAccuracy
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.sync_action
import dev.mj31.logger.client.app.resources.sync_action_again
import dev.mj31.logger.client.app.resources.sync_coverage
import dev.mj31.logger.client.app.resources.sync_details
import dev.mj31.logger.client.app.resources.sync_follow_video
import dev.mj31.logger.client.app.resources.sync_frame_time_apply
import dev.mj31.logger.client.app.resources.sync_frame_time_hint
import dev.mj31.logger.client.app.resources.sync_frame_time_invalid
import dev.mj31.logger.client.app.resources.sync_frame_time_label
import dev.mj31.logger.client.app.resources.sync_hint
import dev.mj31.logger.client.app.resources.sync_no_overlap
import dev.mj31.logger.client.app.resources.sync_origin_frame_time
import dev.mj31.logger.client.app.resources.sync_origin_metadata
import dev.mj31.logger.client.app.resources.sync_origin_screen_clock
import dev.mj31.logger.client.app.resources.sync_origin_selected_entry
import dev.mj31.logger.client.app.resources.sync_state_independent
import dev.mj31.logger.client.app.resources.sync_state_synchronized
import dev.mj31.logger.client.app.resources.sync_unknown_time
import dev.mj31.logger.client.app.resources.sync_unlink
import dev.mj31.logger.client.app.theme.AccentSync
import dev.mj31.logger.client.app.view.format.formatLogTime
import dev.mj31.logger.client.app.view.format.formatVideoPosition
import dev.mj31.logger.client.app.view.input.rememberTextDraft
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mj31.logger.client.app.features.logplayer.sync.FrameTimePickerDialog
import dev.mj31.logger.client.app.resources.sync_frame_time_pick

/**
 * Bottom bar driving the manual synchronization.
 *
 * Until the user presses "Synchronize" the two timelines are completely independent; afterwards the
 * bar reports the mapping and how much of the log session the screencast actually covers.
 *
 * There are two ways to state the correlation: pin the selected record to the playhead, or type the
 * wall clock time the current frame shows, which also covers moments no record describes.
 */
@Composable
fun SyncBar(
    state: LogPlayerState,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        FrameTimeRow(state = state, onIntent = onIntent)

        Spacer(modifier = Modifier.height(height = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = stringResource(
                        resource = if (state.sync.isSynced) {
                            Res.string.sync_state_synchronized
                        } else {
                            Res.string.sync_state_independent
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.sync.isSynced) AccentSync else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = syncDetails(state = state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }

            FollowVideoToggle(state = state, onIntent = onIntent)

            Spacer(modifier = Modifier.width(width = 12.dp))

            AutoSyncMenu(autoSync = state.autoSync, onIntent = onIntent)

            Spacer(modifier = Modifier.width(width = 8.dp))

            if (state.sync.isSynced) {
                TextButton(onClick = { onIntent(LogPlayerIntent.ClearSynchronization) }) {
                    Text(text = stringResource(resource = Res.string.sync_unlink))
                }
                Spacer(modifier = Modifier.width(width = 8.dp))
            }

            Button(
                onClick = { onIntent(LogPlayerIntent.Synchronize) },
                enabled = state.sync.canSynchronize,
            ) {
                Text(
                    text = stringResource(
                        resource = if (state.sync.isSynced) Res.string.sync_action_again else Res.string.sync_action,
                    ),
                )
            }
        }
    }
}

/** Whether the list follows the playhead; only meaningful once the timelines are pinned together. */
@Composable
private fun FollowVideoToggle(state: LogPlayerState, onIntent: (LogPlayerIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(resource = Res.string.sync_follow_video),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = state.followVideo,
            onCheckedChange = { enabled -> onIntent(LogPlayerIntent.SetFollowVideo(enabled = enabled)) },
            enabled = state.sync.isSynced,
        )
    }
}

/**
 * Exact time the current frame shows, typed by the user.
 *
 * The field owns the typed text, as every other input of the workspace does: a value driven from
 * the store arrives one frame late and resets the undo history of the field.
 */
@Composable
private fun FrameTimeRow(state: LogPlayerState, onIntent: (LogPlayerIntent) -> Unit) {
    val draft = rememberTextDraft(external = state.sync.frameTime)
    var isPickerOpen by remember { mutableStateOf(value = false) }

    if (isPickerOpen) {
        FrameTimePickerDialog(
            initial = state.sync.frameTimeDefault,
            onDismiss = { isPickerOpen = false },
            onConfirm = { dateMillis, hour, minute ->
                isPickerOpen = false
                onIntent(LogPlayerIntent.PickFrameTime(dateMillis = dateMillis, hour = hour, minute = minute))
            },
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft.value,
            onValueChange = { typed ->
                draft.value = typed
                onIntent(LogPlayerIntent.UpdateFrameTime(text = typed))
            },
            label = { Text(text = stringResource(resource = Res.string.sync_frame_time_label)) },
            placeholder = { Text(text = stringResource(resource = Res.string.sync_frame_time_hint)) },
            singleLine = true,
            isError = state.sync.frameTimeError,
            // Always present, empty when there is nothing wrong: a supporting line that appears only
            // on an error grows the bar under it and shoves the whole workspace up as the user
            // types, then drops it back the moment the text becomes valid again.
            supportingText = {
                Text(
                    text = if (state.sync.frameTimeError) {
                        stringResource(resource = Res.string.sync_frame_time_invalid)
                    } else {
                        ""
                    },
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(weight = 1f),
        )

        Spacer(modifier = Modifier.width(width = 8.dp))

        TextButton(onClick = { isPickerOpen = true }) {
            Text(text = stringResource(resource = Res.string.sync_frame_time_pick))
        }

        TextButton(
            onClick = { onIntent(LogPlayerIntent.SynchronizeAtFrameTime) },
            enabled = state.sync.canSynchronizeAtFrameTime,
        ) {
            Text(text = stringResource(resource = Res.string.sync_frame_time_apply))
        }
    }
}

@Composable
private fun syncDetails(state: LogPlayerState): String {
    if (!state.sync.isSynced) return stringResource(resource = Res.string.sync_hint)

    val playheadTime = state.sync.logTimeAtPlayhead
        ?.let { instant -> formatLogTime(instant = instant) }
        ?: stringResource(resource = Res.string.sync_unknown_time)
    val covered = state.sync.overlap?.overlap
    val coverage = if (covered == null) {
        stringResource(resource = Res.string.sync_no_overlap)
    } else {
        stringResource(
            resource = Res.string.sync_coverage,
            formatLogTime(instant = covered.start),
            formatLogTime(instant = covered.end),
        )
    }
    val mapping = stringResource(
        resource = Res.string.sync_details,
        formatVideoPosition(positionMillis = state.video.positionMillis),
        playheadTime,
        coverage,
    )
    val origin = syncOrigin(state = state)
    return if (origin.isEmpty()) mapping else "$mapping  |  $origin"
}

/**
 * Where the anchor came from, and what it is worth.
 *
 * An anchor placed by a human is exact by definition and says only how it was placed. One the
 * application found says how far it may be off, because that is the difference between reading a log
 * beside the frame that produced it and reading one beside the second that produced it.
 */
@Composable
private fun syncOrigin(state: LogPlayerState): String = when (state.sync.origin) {
    SyncOrigin.SELECTED_ENTRY -> stringResource(resource = Res.string.sync_origin_selected_entry)
    SyncOrigin.FRAME_TIME -> stringResource(resource = Res.string.sync_origin_frame_time)
    SyncOrigin.VIDEO_METADATA -> stringResource(
        resource = Res.string.sync_origin_metadata,
        formatAccuracy(millis = state.sync.accuracyMillis),
    )

    SyncOrigin.SCREEN_CLOCK -> stringResource(
        resource = Res.string.sync_origin_screen_clock,
        formatAccuracy(millis = state.sync.accuracyMillis),
    )

    null -> ""
}
