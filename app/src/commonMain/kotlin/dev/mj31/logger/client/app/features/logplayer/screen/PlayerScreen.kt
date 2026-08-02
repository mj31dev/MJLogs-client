package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.app.view.MessageBar
import dev.mj31.logger.client.app.view.UiMessage
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.format.FormatWizardDialog

/**
 * Screencast on the left, merged log session on the right, synchronization controls at the bottom.
 *
 * The screen is a pure rendering of [state]; the only thing it produces is intents.
 */
@Composable
fun PlayerScreen(
    state: LogPlayerState,
    frame: State<VideoFrame?>,
    message: UiMessage?,
    onIntent: (LogPlayerIntent) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        if (state.isImporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        SplitWorkspace(
            state = state,
            frame = frame,
            onIntent = onIntent,
            modifier = Modifier.weight(weight = 1f),
        )

        message?.let { notice ->
            MessageBar(message = notice, onDismiss = onDismissMessage)
        }

        SyncBar(state = state, onIntent = onIntent)
    }

    state.formatRequest?.let { request ->
        FormatWizardDialog(request = request, onIntent = onIntent)
    }
}

@Composable
private fun SplitWorkspace(
    state: LogPlayerState,
    frame: State<VideoFrame?>,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        val totalWidthPx = with(LocalDensity.current) { availableWidth.toPx() }
        var splitFraction by remember { mutableStateOf(value = DEFAULT_SPLIT) }

        Row(modifier = Modifier.fillMaxSize()) {
            VideoPane(
                video = state.video,
                frame = frame,
                onIntent = onIntent,
                modifier = Modifier
                    .width(width = availableWidth * splitFraction)
                    .fillMaxHeight(),
            )

            Box(
                modifier = Modifier
                    .width(width = SPLITTER_WIDTH.dp)
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.outline)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            splitFraction = (splitFraction + delta / totalWidthPx)
                                .coerceIn(minimumValue = MIN_SPLIT, maximumValue = MAX_SPLIT)
                        },
                    ),
            )

            LogPane(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxHeight(),
            )
        }
    }
}

private const val DEFAULT_SPLIT = 0.5f
private const val MIN_SPLIT = 0.25f
private const val MAX_SPLIT = 0.75f
private const val SPLITTER_WIDTH = 5
