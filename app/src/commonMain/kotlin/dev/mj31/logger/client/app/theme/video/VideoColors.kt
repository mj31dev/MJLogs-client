package dev.mj31.logger.client.app.theme.video

import androidx.compose.ui.graphics.Color

/**
 * Colours that sit on the video frame rather than on the interface.
 *
 * The frame is the same picture in both schemes, so what is drawn on it cannot follow the scheme:
 * a light scrim over a dark recording hides it, and a letterbox that turns white in the light scheme
 * reads as part of the picture. These are the only colours a screen may take from outside
 * `MaterialTheme.colorScheme`, and only for what is drawn over or around the frame.
 */
object VideoColors {

    /** Around a frame whose shape does not fill its box. */
    val letterbox: Color = Color.Black

    /** Laid over the frame to push it back while something is drawn on it. */
    val scrim: Color = Color.Black.copy(alpha = SCRIM_ALPHA)

    /** Text and strokes drawn on the scrim or the letterbox. */
    val onScrim: Color = Color.White

    /** Secondary text on the scrim or the letterbox: an explanation under a title. */
    val onScrimMuted: Color = Color.White.copy(alpha = MUTED_ALPHA)

    private const val SCRIM_ALPHA = 0.55f
    private const val MUTED_ALPHA = 0.7f
}
