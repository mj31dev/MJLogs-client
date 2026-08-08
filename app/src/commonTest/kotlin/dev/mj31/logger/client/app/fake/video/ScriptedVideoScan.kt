package dev.mj31.logger.client.app.fake.video

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.source.video.VideoScan

/**
 * A recording that decodes nothing.
 *
 * Every frame it hands out carries its own position in [VideoFrame.sequence], which is what lets
 * [ScriptedClockReader] answer as a clock would without a single pixel being drawn. The search for a
 * minute change is arithmetic over readings, and this is what that arithmetic can be tested against
 * exactly — the recognizer meets a real recording in the desktop suite instead.
 */
class ScriptedVideoScan(
    override val durationMillis: Long,
) : VideoScan {

    var frameRequests: Int = 0
        private set

    override suspend fun frameAt(positionMillis: Long): VideoFrame? {
        if (positionMillis < 0 || positionMillis > durationMillis) return null
        frameRequests += 1
        return VideoFrame(width = 1, height = 1, pixels = ByteArray(size = PIXEL_BYTES), sequence = positionMillis)
    }

    override suspend fun close() = Unit

    private companion object {
        const val PIXEL_BYTES = 4
    }
}
