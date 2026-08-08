package dev.mj31.logger.client.app.features.logplayer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.player.VideoStep
import dev.mj31.logger.client.app.view.toImageBitmap
import dev.mj31.logger.client.app.view.format.formatVideoPosition
import dev.mj31.logger.client.app.features.logplayer.state.ui.AutoSyncUiState
import dev.mj31.logger.client.app.features.logplayer.state.ui.VideoUiState
import dev.mj31.logger.client.app.features.logplayer.sync.ClockRegionOverlay
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.video_decoding
import dev.mj31.logger.client.app.resources.video_decoding_description
import dev.mj31.logger.client.app.resources.video_empty_description
import dev.mj31.logger.client.app.resources.video_empty_title
import dev.mj31.logger.client.app.resources.video_frame_description
import dev.mj31.logger.client.app.resources.video_no_file
import dev.mj31.logger.client.app.resources.video_open
import dev.mj31.logger.client.app.resources.video_pause
import dev.mj31.logger.client.app.resources.video_play
import dev.mj31.logger.client.app.resources.video_playback_unavailable
import dev.mj31.logger.client.app.resources.video_replace
import dev.mj31.logger.client.app.resources.video_step_frame_back
import dev.mj31.logger.client.app.resources.video_step_frame_forward
import dev.mj31.logger.client.app.resources.video_step_second_back
import dev.mj31.logger.client.app.resources.video_step_second_forward
import dev.mj31.logger.client.app.resources.video_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Left half of the workspace: the screencast and its transport controls.
 *
 * The pane only knows [VideoUiState] and a frame stream, so any playback backend that implements
 * the domain port can drive it.
 */
@Composable
fun VideoPane(
    video: VideoUiState,
    frame: State<VideoFrame?>,
    autoSync: AutoSyncUiState,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onOpenVideoClick = { onIntent(LogPlayerIntent.RequestVideoImport) }
    Column(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.background)
            .padding(all = 12.dp),
    ) {
        VideoHeader(video = video, onOpenVideoClick = onOpenVideoClick)

        Spacer(modifier = Modifier.height(height = 8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f)
                .clip(shape = RoundedCornerShape(size = 10.dp))
                .background(color = Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            VideoSurface(video = video, frame = frame, onOpenVideoClick = onOpenVideoClick)

            ClockRegionOverlay(
                autoSync = autoSync,
                frameWidth = frame.value?.width ?: 0,
                frameHeight = frame.value?.height ?: 0,
                onIntent = onIntent,
            )
        }

        Spacer(modifier = Modifier.height(height = 8.dp))

        TransportControls(
            video = video,
            onPlayPause = { onIntent(LogPlayerIntent.TogglePlayback) },
            onSeek = { position -> onIntent(LogPlayerIntent.Seek(positionMillis = position)) },
        )

        Spacer(modifier = Modifier.height(height = 4.dp))

        StepControls(video = video, onIntent = onIntent)
    }
}

@Composable
private fun VideoHeader(video: VideoUiState, onOpenVideoClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = stringResource(resource = Res.string.video_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = video.name ?: stringResource(resource = Res.string.video_no_file),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Button(onClick = onOpenVideoClick) {
            Text(
                text = stringResource(
                    resource = if (video.name == null) Res.string.video_open else Res.string.video_replace,
                ),
            )
        }
    }
}

@Composable
private fun VideoSurface(
    video: VideoUiState,
    frame: State<VideoFrame?>,
    onOpenVideoClick: () -> Unit,
) {
    val currentFrame = frame.value
    val image = remember(key1 = currentFrame?.sequence) { currentFrame?.toImageBitmap() }

    when {
        video.errorMessage != null -> VideoPlaceholder(
            title = stringResource(resource = Res.string.video_playback_unavailable),
            description = video.errorMessage,
            actionLabel = null,
            onAction = onOpenVideoClick,
        )

        image != null -> Image(
            bitmap = image,
            contentDescription = stringResource(resource = Res.string.video_frame_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        video.name != null -> VideoPlaceholder(
            title = stringResource(resource = Res.string.video_decoding),
            description = stringResource(resource = Res.string.video_decoding_description, video.name.orEmpty()),
            actionLabel = null,
            onAction = onOpenVideoClick,
        )

        else -> VideoPlaceholder(
            title = stringResource(resource = Res.string.video_empty_title),
            description = stringResource(resource = Res.string.video_empty_description),
            actionLabel = stringResource(resource = Res.string.video_open),
            onAction = onOpenVideoClick,
        )
    }
}

@Composable
private fun VideoPlaceholder(
    title: String,
    description: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(height = 6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(height = 14.dp))
            Button(onClick = onAction) { Text(text = actionLabel) }
        }
    }
}

/**
 * The playhead moved by exact amounts rather than dragged.
 *
 * A slider cannot answer the question this application exists for — which of two frames a log line
 * belongs to — because a pixel of it is worth several frames on any recording longer than a minute.
 * A second is the step for finding roughly the right place; a frame is the step for settling what
 * happened first.
 */
@Composable
private fun StepControls(video: VideoUiState, onIntent: (LogPlayerIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(label = Res.string.video_step_second_back, video = video) {
            onIntent(LogPlayerIntent.StepVideo(step = VideoStep.SECOND, steps = -1))
        }
        StepButton(label = Res.string.video_step_frame_back, video = video) {
            onIntent(LogPlayerIntent.StepVideo(step = VideoStep.FRAME, steps = -1))
        }
        StepButton(label = Res.string.video_step_frame_forward, video = video) {
            onIntent(LogPlayerIntent.StepVideo(step = VideoStep.FRAME, steps = 1))
        }
        StepButton(label = Res.string.video_step_second_forward, video = video) {
            onIntent(LogPlayerIntent.StepVideo(step = VideoStep.SECOND, steps = 1))
        }
    }
}

@Composable
private fun StepButton(label: StringResource, video: VideoUiState, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = video.hasVideo,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(resource = label),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun TransportControls(
    video: VideoUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var scrubPosition by remember { mutableStateOf<Float?>(value = null) }
    val duration = video.durationMillis.coerceAtLeast(minimumValue = 1L)
    val sliderValue = scrubPosition ?: video.positionMillis.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlayPause,
            enabled = video.hasVideo,
        ) {
            Text(
                text = stringResource(
                    resource = if (video.isPlaying) Res.string.video_pause else Res.string.video_play,
                ),
            )
        }

        Spacer(modifier = Modifier.width(width = 12.dp))

        Text(
            text = formatVideoPosition(positionMillis = sliderValue.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
        )

        Slider(
            value = sliderValue.coerceIn(minimumValue = 0f, maximumValue = duration.toFloat()),
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                scrubPosition?.let { onSeek(it.toLong()) }
                scrubPosition = null
            },
            valueRange = 0f..duration.toFloat(),
            enabled = video.hasVideo && video.durationMillis > 0,
            modifier = Modifier
                .weight(weight = 1f)
                .padding(horizontal = 12.dp),
        )

        Text(
            text = formatVideoPosition(positionMillis = video.durationMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
    }
}
