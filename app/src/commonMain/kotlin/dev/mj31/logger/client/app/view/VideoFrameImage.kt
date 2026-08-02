package dev.mj31.logger.client.app.view

import androidx.compose.ui.graphics.ImageBitmap
import dev.mj31.logger.client.domain.player.VideoFrame

/**
 * Converts a decoded BGRA frame into something Compose can draw.
 *
 * Kept as an expect declaration so the domain stays free of any imaging API and so another target
 * can provide its own conversion.
 */
expect fun VideoFrame.toImageBitmap(): ImageBitmap?
