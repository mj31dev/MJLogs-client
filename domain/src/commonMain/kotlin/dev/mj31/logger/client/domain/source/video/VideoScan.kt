package dev.mj31.logger.client.domain.source.video

import dev.mj31.logger.client.domain.player.VideoFrame

/**
 * One video opened for inspection, positioned wherever the caller asks.
 *
 * Frames arrive at their full recorded size rather than scaled down as the player scales them: a
 * status bar clock on a phone recording survives being read only while its digits are still tens of
 * pixels tall.
 *
 * Holds a native decoder, so [close] is not optional.
 */
interface VideoScan {

    val durationMillis: Long

    suspend fun frameAt(positionMillis: Long): VideoFrame?

    suspend fun close()
}
