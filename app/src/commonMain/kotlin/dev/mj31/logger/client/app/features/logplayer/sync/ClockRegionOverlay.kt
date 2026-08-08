package dev.mj31.logger.client.app.features.logplayer.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.ui.AutoSyncUiState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.sync_auto_cancel
import dev.mj31.logger.client.app.resources.sync_auto_region_cancel
import dev.mj31.logger.client.app.resources.sync_auto_region_hint
import dev.mj31.logger.client.app.resources.sync_auto_scanning
import dev.mj31.logger.client.app.theme.AccentSync
import org.jetbrains.compose.resources.stringResource

/**
 * What the automatic synchronization puts over the picture: a progress indicator while it reads, and
 * a rectangle to draw when it could not find the clock by itself.
 *
 * Both sit on top of the frame rather than beside it, because both are about the frame. Neither
 * blocks the player: a scan runs on its own decoder, so the recording stays watchable while it does.
 *
 * The rectangle is measured against the picture, not against the pane. A frame is drawn to fit, so
 * on a tall phone recording in a wide pane most of what the user can point at is black bars, and
 * coordinates taken from the pane would place the clock somewhere off the actual image.
 */
@Composable
fun ClockRegionOverlay(
    autoSync: AutoSyncUiState,
    frameWidth: Int,
    frameHeight: Int,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        autoSync.isSelectingRegion -> ClockRegionPicker(
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            onIntent = onIntent,
            modifier = modifier,
        )

        autoSync.isScanning -> ScanningIndicator(onIntent = onIntent, modifier = modifier)
    }
}

@Composable
private fun ScanningIndicator(onIntent: (LogPlayerIntent) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = SCRIM_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(size = 32.dp), color = AccentSync)
            Text(
                text = stringResource(resource = Res.string.sync_auto_scanning),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { onIntent(LogPlayerIntent.CancelAutoSync) }) {
                Text(text = stringResource(resource = Res.string.sync_auto_cancel))
            }
        }
    }
}

@Composable
private fun ClockRegionPicker(
    frameWidth: Int,
    frameHeight: Int,
    onIntent: (LogPlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var start by remember { mutableStateOf<Offset?>(value = null) }
    var current by remember { mutableStateOf<Offset?>(value = null) }
    var paneWidth by remember { mutableStateOf(value = 0f) }
    var paneHeight by remember { mutableStateOf(value = 0f) }
    val hint = stringResource(resource = Res.string.sync_auto_region_hint)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = SCRIM_ALPHA))
            .semantics { contentDescription = hint }
            .pointerInput(key1 = frameWidth, key2 = frameHeight) {
                paneWidth = size.width.toFloat()
                paneHeight = size.height.toFloat()
                detectDragGestures(
                    onDragStart = { offset ->
                        start = offset
                        current = offset
                    },
                    onDrag = { change, _ -> current = change.position },
                    onDragEnd = {
                        val from = start
                        val to = current
                        start = null
                        current = null
                        if (from != null && to != null) {
                            onIntent(
                                regionIntentOf(
                                    from = from,
                                    to = to,
                                    paneWidth = paneWidth,
                                    paneHeight = paneHeight,
                                    frameWidth = frameWidth,
                                    frameHeight = frameHeight,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        start = null
                        current = null
                    },
                )
            }
            .drawBehind { drawOutline(from = start, to = current) },
        contentAlignment = Alignment.TopCenter,
    ) {
        RegionHint(hint = hint, onIntent = onIntent)
    }
}

@Composable
private fun RegionHint(hint: String, onIntent: (LogPlayerIntent) -> Unit) {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = { onIntent(LogPlayerIntent.CancelClockRegion) }) {
            Text(text = stringResource(resource = Res.string.sync_auto_region_cancel))
        }
    }
}

private fun DrawScope.drawOutline(from: Offset?, to: Offset?) {
    if (from == null || to == null) return
    drawRect(
        color = AccentSync,
        topLeft = Offset(x = minOf(a = from.x, b = to.x), y = minOf(a = from.y, b = to.y)),
        size = sizeOf(
            width = kotlin.math.abs(x = to.x - from.x),
            height = kotlin.math.abs(x = to.y - from.y),
        ),
        style = Stroke(width = STROKE_WIDTH),
    )
}

/**
 * Converts two points on the pane into fractions of the picture.
 *
 * The frame is drawn to fit and centred, so the picture occupies a rectangle inside the pane and the
 * rest is padding; both ends of the drag are measured against that rectangle.
 */
private fun regionIntentOf(
    from: Offset,
    to: Offset,
    paneWidth: Float,
    paneHeight: Float,
    frameWidth: Int,
    frameHeight: Int,
): LogPlayerIntent.SetClockRegion {
    val scale = minOf(a = paneWidth / frameWidth, b = paneHeight / frameHeight)
    val drawnWidth = frameWidth * scale
    val drawnHeight = frameHeight * scale
    val originX = (paneWidth - drawnWidth) / 2f
    val originY = (paneHeight - drawnHeight) / 2f

    val left = ((minOf(a = from.x, b = to.x) - originX) / drawnWidth).coerceIn(range = UNIT)
    val right = ((maxOf(a = from.x, b = to.x) - originX) / drawnWidth).coerceIn(range = UNIT)
    val top = ((minOf(a = from.y, b = to.y) - originY) / drawnHeight).coerceIn(range = UNIT)
    val bottom = ((maxOf(a = from.y, b = to.y) - originY) / drawnHeight).coerceIn(range = UNIT)

    return LogPlayerIntent.SetClockRegion(left = left, top = top, right = right, bottom = bottom)
}

/**
 * [Size] only exposes its two-argument form positionally: the named constructor of the value class
 * takes the packed representation and is internal to Compose.
 */
@Suppress("NamedArguments")
private fun sizeOf(width: Float, height: Float): Size = Size(width, height)

private const val SCRIM_ALPHA = 0.55f
private const val STROKE_WIDTH = 3f
private val UNIT = 0f..1f
